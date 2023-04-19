package sweet;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import org.json.JSONArray;
import org.json.JSONException;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;

public class SweetBE implements BurpExtension {
    private MontoyaApi montoyaAPI;
    private SweetHTTPHandler sweetHTTPHandler;

    @Override
    public void initialize(MontoyaApi api) {
        this.montoyaAPI = api;
        api.logging().logToOutput("Loading Sweet Burp Extension (SweetBE).");
        api.extension().setName("SweetBE");

        SweetTableModel tableModel = new SweetTableModel();
        sweetHTTPHandler = new SweetHTTPHandler(tableModel, montoyaAPI);

        api.userInterface().registerSuiteTab("SweetBE", constructSweetBETab(tableModel));
        api.http().registerHttpHandler(sweetHTTPHandler);
    }

    private void parseTheCandy(String criteriaText) {
        montoyaAPI.logging().logToOutput("Yoohoo! We're parsing some sweet candy:\n");
        try {
            JSONArray jsonArray = new JSONArray(criteriaText);
            montoyaAPI.logging().logToOutput(jsonArray.toString(4) + "\n");
            sweetHTTPHandler.setFindReplaceCriteriaArray(jsonArray);
        } catch (JSONException jsonException) {
            montoyaAPI.logging().logToOutput(jsonException.getMessage());
            throw jsonException;
        }
    }

    private Component constructSweetBETab(SweetTableModel tableModel) {
        // main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane httpReqResSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        UserInterface userInterface = montoyaAPI.userInterface();

        HttpRequestEditor requestViewer = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor responseViewer = userInterface.createHttpResponseEditor(READ_ONLY);

        httpReqResSplitPane.setLeftComponent(requestViewer.uiComponent());
        httpReqResSplitPane.setRightComponent(responseViewer.uiComponent());
        httpReqResSplitPane.setResizeWeight(0.5);

        splitPane.setRightComponent(httpReqResSplitPane);

        // create table of log entries based on the table model
        JTable table = new JTable(tableModel) {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                // show the log entry for the selected row
                HttpRequestResponse httpRequestResponse = tableModel.get(rowIndex);
                requestViewer.setRequest(httpRequestResponse.request());
                responseViewer.setResponse(httpRequestResponse.response());

                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // center-align some of the table columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int columnNumber : new int[]{0, 1, 4, 5, 6, 7, 8, 9}) {
            TableColumn tableColumn = table.getColumnModel().getColumn(columnNumber);
            tableColumn.setCellRenderer(centerRenderer);
//            api.logging().logToOutput("column header:" + tableColumn.getHeaderValue().toString());
//            api.logging().logToOutput("Column Header length is:" + tableColumn.getHeaderValue().toString().length());
//            tableColumn.setMinWidth(13);
//            tableColumn.setMaxWidth(13);
//            tableColumn.setPreferredWidth(13);
//            api.logging().logToOutput("column minwidth is is:" + tableColumn.getMinWidth());
//            api.logging().logToOutput("column maxwitdh is is:" + tableColumn.getMaxWidth());
//            api.logging().logToOutput("Preferred column width is set to:" + tableColumn.getPreferredWidth());
        }

        JScrollPane scrollPane = new JScrollPane(table);
        splitPane.setLeftComponent(scrollPane);

        // create menu bar in our custom tab
        JMenuBar menuBar = new JMenuBar();
        menuBar.setMargin(new Insets(2, 2, 2, 2));

        // create button that will be present in the menu
        JButton searchReplaceCriteriaButton = new JButton("Settings");

        // create window that will pop up when the button is pushed
        JFrame criteriaFrame = new JFrame("Settings - Find & Replace Criteria");
        criteriaFrame.setResizable(false);
        criteriaFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        Container contentPane = criteriaFrame.getContentPane();
        SpringLayout layout = new SpringLayout();
        contentPane.setLayout(layout);

        JTextArea criteriaTextArea = new JTextArea(15, 50);
        criteriaTextArea.setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));
        criteriaTextArea.setLineWrap(true);

        criteriaTextArea.setText("""
                [
                    {
                        "find_what": "USERID=12345",
                        "replace_with": "USERID=1337",
                        "success_string": "200 OK"
                    }
                ]""");

        JScrollPane criteriaTextAreaScrollPane = new JScrollPane(criteriaTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JLabel criteriaLabel = new JLabel("Enter Criteria (JSON) - Case Sensitive");
        JButton saveCriteriaButton = new JButton("Save Criteria");
        contentPane.add(criteriaLabel);
        contentPane.add(criteriaTextAreaScrollPane);
        contentPane.add(saveCriteriaButton);

        layout.putConstraint(SpringLayout.NORTH, criteriaLabel, 6, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, criteriaLabel, 6, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.EAST, criteriaLabel, 6, SpringLayout.EAST, contentPane);

        layout.putConstraint(SpringLayout.NORTH, criteriaTextAreaScrollPane, 6, SpringLayout.SOUTH, criteriaLabel);
        layout.putConstraint(SpringLayout.WEST, criteriaTextAreaScrollPane, 6, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.EAST, contentPane, 6, SpringLayout.EAST, criteriaTextAreaScrollPane);

        layout.putConstraint(SpringLayout.NORTH, saveCriteriaButton, 6, SpringLayout.SOUTH, criteriaTextAreaScrollPane);
        layout.putConstraint(SpringLayout.EAST, saveCriteriaButton, -6, SpringLayout.EAST, contentPane);

        layout.putConstraint(SpringLayout.SOUTH, contentPane, 6, SpringLayout.SOUTH, saveCriteriaButton);

        criteriaFrame.pack();

        // init criteria
        parseTheCandy(criteriaTextArea.getText());

        // add action listeners to buttons
        saveCriteriaButton.addActionListener(e -> {
            // parse criteria
            try {
                parseTheCandy(criteriaTextArea.getText());
                criteriaFrame.setVisible(false);
            } catch (JSONException jsonException) {
                JOptionPane.showMessageDialog(criteriaFrame, jsonException.getMessage(), "Invalid JSON", JOptionPane.ERROR_MESSAGE);
            }
        });

        searchReplaceCriteriaButton.addActionListener(e -> {
            criteriaTextArea.grabFocus();
            criteriaFrame.setLocationRelativeTo(mainSplitPane);
            criteriaFrame.setVisible(true);
        });

        // close the criteria settings on ESC press
        criteriaTextArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
                    criteriaFrame.setVisible(false);
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {}
        });

        menuBar.add(searchReplaceCriteriaButton);

        mainSplitPane.setTopComponent(menuBar);
        mainSplitPane.setBottomComponent(splitPane);
        mainSplitPane.setDividerSize(0);

        return mainSplitPane;
    }
}