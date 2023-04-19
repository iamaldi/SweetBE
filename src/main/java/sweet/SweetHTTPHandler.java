package sweet;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.ContentType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class SweetHTTPHandler implements HttpHandler {
    private final SweetTableModel sweetTableModel;
    private final MontoyaApi montoyaApi;
    private Integer messageId;
    private JSONArray findReplaceCriteriaArray;

    public SweetHTTPHandler(SweetTableModel sweetTableModel, MontoyaApi montoyaApi) {
        this.messageId = -1;
        this.sweetTableModel = sweetTableModel;
        this.montoyaApi = montoyaApi;
        this.findReplaceCriteriaArray = new JSONArray();
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {

        // ignore requests coming from Repeater, Intruder or Extensions;
        // filter only organic traffic from Proxy
        if (requestToBeSent.toolSource().isFromTool(ToolType.PROXY) && !requestToBeSent.toolSource().isFromTool(ToolType.EXTENSIONS)) {

            String requestToBeSentPath = requestToBeSent.path();
            String requestToBeSentHeadersString = requestToBeSent.headers().toString();
            String requestToBeSentString = requestToBeSent.bodyToString();
            JSONObject criteriaJsonObject;
            String findWhatCriteria;
            boolean foundCriteria = false;

            montoyaApi.logging().logToOutput(requestToBeSentPath);
            montoyaApi.logging().logToOutput(requestToBeSentHeadersString);

            for (int i = 0; i < findReplaceCriteriaArray.length(); i++) {
                criteriaJsonObject = findReplaceCriteriaArray.getJSONObject(i);
                findWhatCriteria = criteriaJsonObject.getString("find_what");

                // search request path
                if (montoyaApi.utilities().urlUtils().decode(requestToBeSentPath).contains(findWhatCriteria)){
                    foundCriteria = true;
                    requestToBeSentPath = requestToBeSentPath.replaceAll(findWhatCriteria, montoyaApi.utilities().urlUtils().encode(criteriaJsonObject.getString("replace_with")));
                }

                // TODO: search request headers

                // search request body
                if (requestToBeSentString.contains(findWhatCriteria)) {
                    foundCriteria = true;

                    // URL-encode the replacement criteria when the request has "Content-Type: application/x-www-form-urlencoded"
                    String replaceWithCriteria = criteriaJsonObject.getString("replace_with");
                    if (Objects.requireNonNull(requestToBeSent.contentType()) == ContentType.URL_ENCODED) {
                        replaceWithCriteria = montoyaApi.utilities().urlUtils().encode(replaceWithCriteria);
                    }

                    requestToBeSentString = requestToBeSentString.replaceAll(findWhatCriteria, replaceWithCriteria);
                }
            }
            montoyaApi.logging().logToOutput(requestToBeSentPath);

            // send a separate modified request when we match at least one find&replace criteria
            if (foundCriteria) {
                messageId = requestToBeSent.messageId() + 1;
                montoyaApi.http().sendRequest(requestToBeSent.withBody(requestToBeSentString).withPath(requestToBeSentPath));
            }
        }
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {

        if (messageId.equals(responseReceived.messageId())) {
            messageId = -1; // clear message identifier, so we can use it in next request
            HttpRequestResponse httpRequestResponse = HttpRequestResponse.httpRequestResponse(responseReceived.initiatingRequest(), responseReceived);
            for (int i = 0; i < findReplaceCriteriaArray.length(); i++) {
                if (httpRequestResponse.response().toString().toLowerCase().contains(findReplaceCriteriaArray.getJSONObject(i).getString("success_string").toLowerCase())) {
                    httpRequestResponse.annotations().setNotes(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime()) + ";True");
                } else {
                    httpRequestResponse.annotations().setNotes(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime()) + ";False");
                }

            }
            sweetTableModel.add(httpRequestResponse);
            httpRequestResponse = null; // empty it so GC can clean it
        }
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    public synchronized void setFindReplaceCriteriaArray(JSONArray findReplaceCriteriaArray) {
        this.findReplaceCriteriaArray = findReplaceCriteriaArray;
    }
}