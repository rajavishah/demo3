package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import com.example.demo.model.Plan;

import redis.clients.jedis.Jedis;

@Service
public class PlanService {

	final static String PLAN = "PLAN";
	// private RedisTemplate<String, Plan> redisTemplate;
	private HashOperations hashOperations; // to access Redis cache
	// public PlanService(RedisTemplate<String, Plan> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//        hashOperations = redisTemplate.opsForHash();
//    }
	private Jedis jedis;
	ETagHandler eTagService;

	public PlanService(Jedis jedis, ETagHandler eTagService) {
		this.jedis = jedis;
		this.eTagService = eTagService;
	}

	public String save(JSONObject plan, String key) throws JSONException {
		convertToMap(plan);
		return this.setEtag(key, plan);
	}

	public Map<String, Plan> findAll() {
		return hashOperations.entries(PLAN);
	}

	public void delete(String id) {
		getOrDeleteData(id, null, true);
	}

	public Map<String, Map<String, Object>> convertToMap(JSONObject jsonObject) throws JSONException {

		Map<String, Map<String, Object>> map = new HashMap<>();
		Map<String, Object> valueMap = new HashMap<>();
		Iterator<String> iterator = jsonObject.keys();

		while (iterator.hasNext()) {

			String redisKey = jsonObject.get("objectType") + ":" + jsonObject.get("objectId");
			String key = iterator.next();
			Object value = jsonObject.get(key);

			if (value instanceof JSONObject) {

				value = convertToMap((JSONObject) value);
				HashMap<String, Map<String, Object>> val = (HashMap<String, Map<String, Object>>) value;
				jedis.sadd(redisKey + ":" + key, val.entrySet().iterator().next().getKey());
				jedis.close();

			} else if (value instanceof JSONArray) {
				value = convertToList((JSONArray) value);
				for (HashMap<String, HashMap<String, Object>> entry : (List<HashMap<String, HashMap<String, Object>>>) value) {
					for (String listKey : entry.keySet()) {
						jedis.sadd(redisKey + ":" + key, listKey);
						jedis.close();
						System.out.println(redisKey + ":" + key + " : " + listKey);
					}
				}
			} else {
				jedis.hset(redisKey, key, value.toString());
				jedis.close();
				valueMap.put(key, value);
				map.put(redisKey, valueMap);
			}
		}
//        System.out.println("MAP: " + map.toString());
		return map;
	}

	private List<Object> convertToList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if (value instanceof JSONArray) {
				value = convertToList((JSONArray) value);
			} else if (value instanceof JSONObject) {
				value = convertToMap((JSONObject) value);
			}
			list.add(value);
		}
		return list;
	}

	private boolean isStringInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	public Map<String, Object> getPlan(String key) {
		Map<String, Object> outputMap = new HashMap<String, Object>();
		getOrDeleteData(key, outputMap, false);
		return outputMap;
	}

	public boolean checkIfKeyExists(String objectKey) {
		return jedis.exists(objectKey);

	}

	private Map<String, Object> getOrDeleteData(String redisKey, Map<String, Object> outputMap, boolean isDelete) {
		Set<String> keys = jedis.keys(redisKey + ":*");
		keys.add(redisKey);
		jedis.close();
		for (String key : keys) {
			if (key.equals(redisKey)) {
				if (isDelete) {
					jedis.del(new String[] { key });
					jedis.close();
				} else {
					Map<String, String> val = jedis.hgetAll(key);
					jedis.close();
					for (String name : val.keySet()) {
						if (!name.equalsIgnoreCase("eTag")) {
							outputMap.put(name,
									isStringInt(val.get(name)) ? Integer.parseInt(val.get(name)) : val.get(name));
						}
					}
				}

			} else {
				String newStr = key.substring((redisKey + ":").length());
				Set<String> members = jedis.smembers(key);
				jedis.close();
				if (members.size() > 1 || newStr.equals("linkedPlanServices")) {
					List<Object> listObj = new ArrayList<Object>();
					for (String member : members) {
						if (isDelete) {
							getOrDeleteData(member, null, true);
						} else {
							Map<String, Object> listMap = new HashMap<String, Object>();
							listObj.add(getOrDeleteData(member, listMap, false));

						}
					}
					if (isDelete) {
						jedis.del(new String[] { key });
						jedis.close();
					} else {
						outputMap.put(newStr, listObj);
					}

				} else {
					if (isDelete) {
						jedis.del(new String[] { members.iterator().next(), key });
						jedis.close();
					} else {
						Map<String, String> val = jedis.hgetAll(members.iterator().next());
						jedis.close();
						Map<String, Object> newMap = new HashMap<String, Object>();
						for (String name : val.keySet()) {
							newMap.put(name,
									isStringInt(val.get(name)) ? Integer.parseInt(val.get(name)) : val.get(name));
						}
						outputMap.put(newStr, newMap);
					}
				}
			}
		}
		return outputMap;
	}

	public String getEtag(String key) {
		return jedis.hget(key, "eTag");
	}

	public String setEtag(String key, JSONObject jsonObject) {
		String eTag = eTagService.getETag(jsonObject);
		jedis.hset(key, "eTag", eTag);
		return eTag;
	}

	public void update(JSONObject object) {
		traverseNode(object);
	}

	public Map<String, Map<String, Object>> traverseNode(JSONObject jsonObject) {

		Map<String, Map<String, Object>> objMap = new HashMap<>();
		Map<String, Object> valueMap = new HashMap<>();
		Iterator<String> iterator = jsonObject.keySet().iterator();

		// traverse all attributes for store
		while (iterator.hasNext()) {

			String redisKey = jsonObject.get("objectType") + ":" + jsonObject.get("objectId");
			String key = iterator.next();
			Object value = jsonObject.get(key);

			// type - Object
			if (value instanceof JSONObject) {

				value = traverseNode((JSONObject) value);
				Map<String, Map<String, Object>> val = (HashMap<String, Map<String, Object>>) value;
				String transitiveKey = redisKey + ":" + key;
				jedis.sadd(transitiveKey, val.entrySet().iterator().next().getKey());
				jedis.close();

			} else if (value instanceof org.json.JSONArray) {

				// type - Array
				value = convertToList((org.json.JSONArray) value);

				List<HashMap<String, HashMap<String, Object>>> formatList = (List<HashMap<String, HashMap<String, Object>>>) value;
				formatList.forEach((listObject) -> {

					listObject.entrySet().forEach((listEntry) -> {

						jedis.sadd(redisKey + ":" + key, listEntry.getKey());
						jedis.close();
						System.out.println(redisKey + ":" + key + " : " + listEntry.getKey());

					});

				});
			} else {
				jedis.hset(redisKey, key, value.toString());
				jedis.close();

				valueMap.put(key, value);
				objMap.put(redisKey, valueMap);
			}

		}

		return objMap;
	}

}
