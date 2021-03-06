/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.webserver.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.supertokens.Main;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.session.Session;
import io.supertokens.session.accessToken.AccessTokenSigningKey;
import io.supertokens.session.info.SessionInformationHolder;
import io.supertokens.webserver.InputParser;
import io.supertokens.webserver.WebserverAPI;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

public class SessionAPI extends WebserverAPI {
    private static final long serialVersionUID = 7142317017402226537L;

    public SessionAPI(Main main) {
        super(main);
    }

    @Override
    public String getPath() {
        return "/session";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        JsonObject input = InputParser.parseJsonObjectOrThrowError(req);
        String userId = InputParser.parseStringOrThrowError(input, "userId", false);
        assert userId != null;
        JsonObject userDataInJWT = InputParser.parseJsonObjectOrThrowError(input, "userDataInJWT", false);
        assert userDataInJWT != null;
        JsonObject userDataInDatabase = InputParser.parseJsonObjectOrThrowError(input, "userDataInDatabase", false);
        assert userDataInDatabase != null;

        try {
            SessionInformationHolder sessionInfo = Session
                    .createNewSession(main, userId, userDataInJWT, userDataInDatabase,
                            super.getVersionFromRequest(req));

            JsonObject result = new JsonParser().parse(new Gson().toJson(sessionInfo)).getAsJsonObject();

            if (super.getVersionFromRequest(req).equals("1.0")) {
                result.getAsJsonObject("accessToken").remove("sameSite");
                result.getAsJsonObject("refreshToken").remove("sameSite");
                result.getAsJsonObject("idRefreshToken").remove("sameSite");
                result.getAsJsonObject("idRefreshToken").remove("cookiePath");
                result.getAsJsonObject("idRefreshToken").remove("cookieSecure");
                result.getAsJsonObject("idRefreshToken").remove("domain");
            }

            result.addProperty("status", "OK");
            result.addProperty("jwtSigningPublicKey", AccessTokenSigningKey.getInstance(main).getKey().publicKey);
            result.addProperty("jwtSigningPublicKeyExpiryTime",
                    AccessTokenSigningKey.getInstance(main).getKeyExpiryTime());
            super.sendJsonResponse(200, result, resp);
        } catch (NoSuchAlgorithmException | StorageQueryException | InvalidKeyException | InvalidKeySpecException |
                StorageTransactionLogicException | SignatureException | IllegalBlockSizeException |
                BadPaddingException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (!super.getVersionFromRequest(req).equals("1.0")) {
            throw new ServletException(
                    new BadRequestException(
                            "/session DELETE is only available in CDI 1.0. Please call /session/remove POST instead"));
        }
        JsonObject input = InputParser.parseJsonObjectOrThrowError(req);

        String userId = InputParser.parseStringOrThrowError(input, "userId", true);

        String sessionHandle = InputParser.parseStringOrThrowError(input, "sessionHandle", true);

        JsonArray arr = InputParser.parseArrayOrThrowError(input, "sessionHandles", true);
        String[] sessionHandles = null;
        if (arr != null) {
            sessionHandles = new String[arr.size()];
            for (int i = 0; i < sessionHandles.length; i++) {
                String session = InputParser.parseStringFromElementOrThrowError(arr.get(i), "sessionHandles", false);
                sessionHandles[i] = session;
            }
        }

        int numberOfNullItems = 0;
        if (userId != null) {
            numberOfNullItems++;
        }
        if (sessionHandle != null) {
            numberOfNullItems++;
        }
        if (sessionHandles != null) {
            numberOfNullItems++;
        }
        if (numberOfNullItems == 0 || numberOfNullItems > 1) {
            throw new ServletException(new BadRequestException(
                    "Invalid JSON input - use one of userId, sessionHandle, or sessionHandles array"));
        }

        if (userId != null) {
            try {
                int numberOfSessionsRevoked = Session.revokeAllSessionsForUser(main, userId).length;
                JsonObject result = new JsonObject();
                result.addProperty("status", "OK");
                result.addProperty("numberOfSessionsRevoked", numberOfSessionsRevoked);
                super.sendJsonResponse(200, result, resp);
            } catch (StorageQueryException e) {
                throw new ServletException(e);
            }
        } else if (sessionHandle != null) {
            try {
                int numberOfSessionsRevoked = Session
                        .revokeSessionUsingSessionHandles(main, new String[]{sessionHandle}).length;
                JsonObject result = new JsonObject();
                result.addProperty("status", "OK");
                result.addProperty("numberOfSessionsRevoked", numberOfSessionsRevoked);
                super.sendJsonResponse(200, result, resp);
            } catch (StorageQueryException e) {
                throw new ServletException(e);
            }
        } else {
            try {
                int numberOfSessionsRevoked = Session.revokeSessionUsingSessionHandles(main, sessionHandles).length;
                JsonObject result = new JsonObject();
                result.addProperty("status", "OK");
                result.addProperty("numberOfSessionsRevoked", numberOfSessionsRevoked);
                super.sendJsonResponse(200, result, resp);
            } catch (StorageQueryException e) {
                throw new ServletException(e);
            }
        }
    }
}
