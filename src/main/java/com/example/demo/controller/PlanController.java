package com.example.demo.controller;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DemoApplication;
import com.example.demo.exceptions.PlanNotFoundException;
import com.example.demo.jsonSchemaBase.BaseJsonSchemaValidator;
import com.example.demo.model.Plan;
import com.example.demo.service.AuthorizeService;
import com.example.demo.service.PlanService;

@RestController
public class PlanController {

	final static String PLAN = "/plan";

	@Autowired
	private PlanService service;

	AuthorizeService authorizeService;

	private BaseJsonSchemaValidator schemaValidator;

	@Autowired
	private RabbitTemplate template;

	public PlanController(PlanService ps, BaseJsonSchemaValidator schemaValidator, AuthorizeService authorizeService) {
		this.service = ps;
		this.schemaValidator = schemaValidator;
		this.authorizeService = authorizeService;
	}

	@RequestMapping(method = RequestMethod.GET, value = PLAN)
	ResponseEntity getPlan(@RequestHeader(value = "authorization") String token) {

		try {
			String result = authorizeService.authorize(token);
			if (!result.equals("VALID_TOKEN")) {
				return new ResponseEntity<>("Invalid Token", HttpStatus.FORBIDDEN);
			}
			Map<String, Plan> plans = service.findAll();
			return new ResponseEntity<>("Success", HttpStatus.OK);
		} catch (PlanNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{objectType}/{id}")
	ResponseEntity getPlan(@PathVariable String objectType, @PathVariable String id,
			@RequestHeader HttpHeaders requestHeaders) throws PlanNotFoundException {
		try {

			String token = requestHeaders.getFirst("Authorization");
			String result = authorizeService.authorize(token);
			if (!result.equals("VALID_TOKEN")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new JSONObject().put("msg", "Unauthorized!").toString());
			}
			String key = objectType + ":" + id;
			Map<String, Object> plan = service.getPlan(key);
			if (plan == null || plan.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new JSONObject().put("msg", "Plan not found!").toString());
			} else {

				String ifNotMatch;
				try {
					ifNotMatch = requestHeaders.getFirst("If-None-Match");
				} catch (Exception e) {
					return new ResponseEntity<>("", HttpStatus.NOT_MODIFIED);
				}

				String actualEtag = service.getEtag(key);
				if (ifNotMatch != null && ifNotMatch.equals(actualEtag)) {
					return new ResponseEntity<>("", HttpStatus.NOT_MODIFIED);
				}

				if (objectType.equals("plan")) {
					return ResponseEntity.status(HttpStatus.OK).eTag(actualEtag).body(plan);
				} else {
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body(new JSONObject().put("msg", "Incorrect URI!").toString());
				}

			}

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = PLAN)
	ResponseEntity addPlan(@RequestBody String request, @RequestHeader HttpHeaders requestHeaders) {

		try {

			String token = requestHeaders.getFirst("Authorization");
			String result = authorizeService.authorize(token);
			if (!result.equals("VALID_TOKEN")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new JSONObject().put("msg", "Unauthorized!").toString());
			}
			JSONObject jsonPlan = new JSONObject(request);

			if (!schemaValidator.validateSchema(jsonPlan)) {
				String error = schemaValidator.getErrorMessage(jsonPlan);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						new JSONObject().put("msg", "Invalid JSON Format & the error message is " + error).toString());
			}
			String key = jsonPlan.get("objectType") + ":" + jsonPlan.get("objectId");
			boolean existingPlan = service.checkIfKeyExists(key);
			if (!existingPlan) {
				String eTag = service.save(jsonPlan, key);
				JSONObject obj = new JSONObject();
				obj.put("ObjectId", jsonPlan.get("objectId"));

				Map<String, String> actionMap = new HashMap<>();
				actionMap.put("operation", "SAVE");
				actionMap.put("body", request);
//                System.out.println("Sending message: " + actionMap);

				template.convertAndSend(DemoApplication.queueName, actionMap);

				return ResponseEntity.created(new URI("/plan/" + key)).eTag(eTag)
						.body(new JSONObject().put("msg", "Created the new plan").toString());

			} else {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new JSONObject().put("msg", "Plan with id already exists").toString());
			}
		} catch (RuntimeException e) {
			return new ResponseEntity<>("Bad Request for Runtime", HttpStatus.BAD_REQUEST);
		} catch (URISyntaxException e) {
			return new ResponseEntity<>("Bad Request for URI", HttpStatus.BAD_REQUEST);
		} catch (FileNotFoundException e) {
			return new ResponseEntity<>("Bad Request for file not found", HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{objectType}/{id}")
	ResponseEntity deletePlan(@PathVariable String id, @PathVariable String objectType,
			@RequestHeader HttpHeaders requestHeaders) {
		try {
			String token = requestHeaders.getFirst("Authorization");
			String result = authorizeService.authorize(token);
			if (!result.equals("VALID_TOKEN")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new JSONObject().put("msg", "Unauthorized!").toString());
			}
			String key = objectType + ":" + id;
			boolean existingPlan = service.checkIfKeyExists(key);
			if (!existingPlan) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new JSONObject().put("msg", "Plan not found!").toString());
			}

			String actualEtag = service.getEtag(key);
			String eTag = requestHeaders.getFirst("If-Match");
			if (eTag == null || eTag.isEmpty()) {
				return new ResponseEntity<>("E-Tag not provided!", HttpStatus.BAD_REQUEST);
			}
			if (eTag != null && !eTag.equals(actualEtag)) {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag).body(
						new JSONObject().put("msg", "Plan cannot be found or has been already deleted!!").toString());
			}

			Map<String, Object> plan = service.getPlan(key);
			Map<String, String> actionMap = new HashMap<>();
			actionMap.put("operation", "DELETE");
			actionMap.put("body", new JSONObject(plan).toString());

			template.convertAndSend(DemoApplication.queueName, actionMap);

			service.delete(key);

			return ResponseEntity.status(HttpStatus.OK)
					.body(new JSONObject().put("msg", "Deleted Successfully!").toString());

		} catch (PlanNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new JSONObject().put("msg", "Plan not found!").toString());
		}
	}

	@RequestMapping(method = RequestMethod.PATCH, value = "/{objectType}/{id}")
	ResponseEntity updatePlan(@PathVariable String id, @PathVariable String objectType, @RequestBody String jsonData,
			@RequestHeader HttpHeaders requestHeaders) throws InterruptedException {
		try {

			String token = requestHeaders.getFirst("Authorization");
			String result = authorizeService.authorize(token);
			if (!result.equals("VALID_TOKEN")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new JSONObject().put("msg", "Unauthorized!").toString());
			}

			if (jsonData == null || jsonData.isEmpty()) {
				return new ResponseEntity<>("Plan not provided", HttpStatus.NO_CONTENT);
			}
			JSONObject jsonPlan = new JSONObject(jsonData);
			String key = objectType + ":" + id;

			if (!service.checkIfKeyExists(key)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new JSONObject().put("msg", "Plan not found!").toString());
			}

			String actualEtag = service.getEtag(key);
			String eTag = requestHeaders.getFirst("If-Match");
			if (eTag == null || eTag.isEmpty()) {
				return new ResponseEntity<>("E-Tag not provided!", HttpStatus.BAD_REQUEST);
			}
			if (eTag != null && !eTag.equals(actualEtag)) {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag)
						.body(new JSONObject().put("msg", "Plan has been already updated!!").toString());
			}

			service.update(jsonPlan);

//			Thread.sleep(3000);
			Map<String, Object> plan = this.service.getPlan(key);
			System.out.println("plan after patch: " + plan);
			Map<String, String> actionMap = new HashMap<>();
			actionMap.put("operation", "SAVE");
			actionMap.put("body", new JSONObject(plan).toString());

			System.out.println("Sending message: " + actionMap);

			template.convertAndSend(DemoApplication.queueName, actionMap);

			return ResponseEntity.status(HttpStatus.OK)
					.body(new JSONObject().put("msg", "Plan patched successfully!").toString());

		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

//	@PatchMapping("/{object}/{id}")
//	public ResponseEntity<String> patchPlan(@PathVariable String object, @PathVariable String id,
//			@Nullable @RequestHeader(value = "Authorization") String idToken,
//			@Nullable @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
//			@RequestBody(required = false) String reqJson) {
//
////        logger.info("PATCHING PLAN: " + object + ":" + id);
//
//		// Authorization
//		if (idToken == null || idToken.isBlank()) {
//			logger.error("TOKEN AUTHORIZATION - token missing");
//
//			return new ResponseEntity<>(new JSONObject().put("message", "Missing Token").toString(),
//					HttpStatus.UNAUTHORIZED);
//		}
//		if (!authService.authorize(idToken.substring(7))) {
////            logger.error("TOKEN AUTHORIZATION - google token expired");
//			return new ResponseEntity<>(new JSONObject().put("Message", "Invalid Token").toString(),
//					HttpStatus.BAD_REQUEST);
//		}
//
//		logger.info("TOKEN AUTHORIZATION SUCCESSFUL");
//
//		JSONObject newPlan = new JSONObject(reqJson);
//
//		try {
//			logger.info(reqJson);
//			planSchema.validateSchema(newPlan, SchemaValidator.patchPath);
//		} catch (Exception e) {
//			logger.info("VALIDATING ERROR: SCHEMA NOT MATCH - " + e.getMessage());
//			return ResponseEntity.badRequest().body(e.getMessage());
//		}
//
//		String intervalKey = object + ":" + id;
//
//		if (!planService.hasKey(intervalKey)) {
//			logger.info("PATCH PLAN: " + intervalKey + " does not exist");
//			return ResponseEntity.status(HttpStatus.NOT_FOUND)
//					.body(new JSONObject().put("Message", "ObjectId does not exist").toString());
//		}
//
//		// Check Etag
//		String planEtag = planService.getEtag(intervalKey, "eTag");
//
//		if (ifMatch == null) {
//			return new ResponseEntity<>(new JSONObject().put("message", "eTag not provided").toString(),
//					HttpStatus.BAD_REQUEST);
//		}
//
//		if (!ifMatch.equals(planEtag)) {
//			return new ResponseEntity<>(new JSONObject().put("message", "Etag does not match").toString(),
//					HttpStatus.PRECONDITION_FAILED);
//		}
//
//		JSONObject patchNewPlan = new JSONObject(reqJson);
//
//		planService.update(intervalKey, patchNewPlan);
//
//		// Send message to queue for index update
//		Map<String, String> message = new HashMap<>();
//		message.put("operation", "SAVE");
//		message.put("body", reqJson);
//
//		System.out.println("Sending message: " + message);
//		rabbitTemplate.convertAndSend(DemoApplication.queueName, message);
//
//		logger.info("PATCH PLAN : " + intervalKey + " updates successfully");
//		return ResponseEntity.status(HttpStatus.OK).header("ETag", encryptionService.encrypt(patchNewPlan.toString()))
//				.body(new JSONObject().put("message", "Updated Successfully").toString());
//	}

}