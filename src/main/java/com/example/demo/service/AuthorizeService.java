package com.example.demo.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
public class AuthorizeService {

    static RSAKey rsaPublicJWK = null;
    static RSAKey rsaJWK = null;

    static {
        try {
            rsaJWK = new RSAKeyGenerator(2048).keyID("123").generate();
            rsaPublicJWK = rsaJWK.toPublicJWK();
        } catch (JOSEException e) {
            e.printStackTrace();
        }
    }

    public String generateToken() throws JOSEException {

        JWSSigner signer = new RSASSASigner(rsaJWK);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .expirationTime(new Date(System.currentTimeMillis() * 1000 * 60 * 2))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
                claimsSet);

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public String authorize(String authorization) {
        if (authorization == null || authorization.isEmpty()) return "NO_TOKEN_FOUND";
        if (!authorization.contains("Bearer ")) return "INVALID_FORMAT";

        String token = authorization.split(" ")[1];

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(this.rsaPublicJWK);

            if (!signedJWT.verify(verifier)) return "INVALID_TOKEN";

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (new Date().after(expirationTime)) {
                return "TOKEN_EXPIRED";
            }
        } catch (JOSEException | ParseException e) {
            return "INVALID_TOKEN";
        }

        return "VALID_TOKEN";
    }
}
