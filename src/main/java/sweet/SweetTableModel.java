package sweet;

import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.HttpRequestResponse;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class SweetTableModel extends AbstractTableModel  {
    private final List<HttpRequestResponse> httpRequestResponses;

    public SweetTableModel() {
        this.httpRequestResponses = new ArrayList<>();
    }

    @Override
    public synchronized int getRowCount() {
        return httpRequestResponses.size();
    }

    @Override
    public int getColumnCount() {
        return 10;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "#";
            case 1 -> "Method";
            case 2 -> "Host";
            case 3 -> "Endpoint";
            case 4 -> "Req. Content-Type";
            case 5 -> "Status Code";
            case 6 -> "Content-Length";
            case 7 -> "Res. Content-Type";
            case 8 -> "Datetime";
            case 9 -> "Success";
            default -> "";
        };
    }

    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        HttpRequestResponse httpRequestResponse = httpRequestResponses.get(rowIndex);
        String[] requestResponseAnnotations = httpRequestResponse.annotations().notes().split(";");

        return switch (columnIndex) {
            case 0 -> rowIndex;
            case 1 -> httpRequestResponse.request().method();
            case 2 -> httpRequestResponse.request().httpService().host();
            case 3 -> httpRequestResponse.request().path();
            case 4 -> httpRequestResponse.request().contentType().toString().toLowerCase();
            case 5 -> httpRequestResponse.response().statusCode();
            case 6 -> httpRequestResponse.response().body().getBytes().length;
            case 7 -> httpRequestResponse.response().statedMimeType();
            case 8 -> requestResponseAnnotations[0];
            case 9 -> requestResponseAnnotations[1];
            default -> "";
        };
    }

    public synchronized void add(HttpRequestResponse httpRequestResponse) {
        int index = httpRequestResponses.size();
        httpRequestResponses.add(httpRequestResponse);
        fireTableRowsInserted(index, index);
    }

    public synchronized HttpRequestResponse get(int rowIndex) {
        return httpRequestResponses.get(rowIndex);
    }
}