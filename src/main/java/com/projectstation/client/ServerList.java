package com.projectstation.client;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ServerList extends JDialog {
    private static final int COLUMN_INDEX_NAME = 0;
    private static final int COLUMN_INDEX_HOSTNAME = 1;
    private static final int COLUMN_INDEX_PLAYERS = 2;
    private static final int COLUMN_INDEX_DESC = 3;

    private JPanel contentPane;
    private JTable tblServerList;
    private JTextPane textPane1;
    private Timer refreshTimer;

    private String masterHost;
    private int masterPort;

    private String connectHost = null;

    public ServerList(String masterHost, int masterPort) {
        setContentPane(contentPane);
        setModal(true);
        setTitle("Project Station Server List");
        setResizable(false);
        setPreferredSize(new Dimension(700, 300));

        TableColumn serverName = new TableColumn(COLUMN_INDEX_NAME);
        serverName.setHeaderValue("Server Name");
        serverName.setPreferredWidth(200);

        tblServerList.setDefaultEditor(Object.class, null);
        tblServerList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    DefaultTableModel model = (DefaultTableModel) tblServerList.getModel();
                    connect((String)model.getValueAt(row, COLUMN_INDEX_HOSTNAME));
                }
            }
        });

        tblServerList.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {
                // do some actions here, for example
                // print first column value from selected row
                textPane1.setText(tblServerList.getModel().getValueAt(tblServerList.getSelectedRow(), COLUMN_INDEX_DESC).toString());
            }
        });


        DefaultTableModel model = (DefaultTableModel) tblServerList.getModel();
        model.addColumn("Server Name");
        model.addColumn("Host");
        model.addColumn("Players");
        model.addColumn("Description");
        tblServerList.getColumnModel().removeColumn(tblServerList.getColumnModel().getColumn(3));

        doLayout();

        this.masterHost = masterHost;
        this.masterPort = masterPort;

        textPane1.setText("Please Select a Server");

        refreshTimer = new Timer(5000, (e) -> {
            refreshServerList();
        });
        refreshTimer.setInitialDelay(1000);
        refreshTimer.start();

        pack();
        setLocationRelativeTo(null);
    }

    public String getConnectHost() {
        return connectHost;
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void connect(String serverName) {
        connectHost = serverName;
        this.setVisible(false);
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    private boolean updateRow(JSONObject desc) {
        String name = desc.getString("name");
        String host = getConnectString(desc);
        String players = getPlayerCountString(desc);
        String description = desc.getString("description");

        DefaultTableModel model = (DefaultTableModel) tblServerList.getModel();

        for(int i = 0; i < model.getRowCount(); i++) {
            if (host.compareToIgnoreCase((String)model.getValueAt(i, COLUMN_INDEX_HOSTNAME)) == 0) {
                model.setValueAt(name, i, COLUMN_INDEX_NAME);
                model.setValueAt(host, i, COLUMN_INDEX_HOSTNAME);
                model.setValueAt(players, i, COLUMN_INDEX_PLAYERS);
                model.setValueAt(description, i, COLUMN_INDEX_DESC);
                return true;
            }
        }

        return false;
    }

    private String getConnectString(JSONObject o) {
        return o.getString("ip") + ":" + o.getInt("port");
    }

    private String getPlayerCountString(JSONObject o) {
        return o.getInt("players") + "/" + o.getInt("max_players");
    }

    private void refreshServerList() {
        //Remove unlisted rows.
        DefaultTableModel model = (DefaultTableModel) tblServerList.getModel();

        if(model.getColumnCount() < 3)
            return;

        try {
            JSONArray list = readJsonFromUrl(String.format("http://%s:%d", masterHost, masterPort));

            ArrayList<JSONObject> newRows = new ArrayList<>();
            ArrayList<String> listed_hosts = new ArrayList<>();

            for(int i = 0; i < list.length(); i++) {
                JSONObject serverDesc = list.getJSONObject(i);
                if(!updateRow(serverDesc))
                    newRows.add(serverDesc);

                listed_hosts.add(getConnectString(serverDesc));
            }


            for(int i = 0; i < model.getRowCount(); i++) {
                if(!listed_hosts.contains(model.getValueAt(i, COLUMN_INDEX_HOSTNAME))){
                    model.removeRow(i);
                    i--;
                }
            }

            //Add new rows
            for(JSONObject server : newRows) {
                model.addRow(new Object[] {server.getString("name"), getConnectString(server), getPlayerCountString(server), server.getString("description")});
            }
        } catch(IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // Set cross-platform Java L&F (also called "Metal")
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException e) {
            // handle exception
        }
        catch (ClassNotFoundException e) {
            // handle exception
        }
        catch (InstantiationException e) {
            // handle exception
        }
        catch (IllegalAccessException e) {
            // handle exception
        }

        try {
            FileInputStream fis = new FileInputStream("server.json");
            JSONObject cfg = new JSONObject(new JSONTokener(fis)).getJSONObject("master_list");

            ServerList dialog = new ServerList(cfg.getString("host"), cfg.getInt("port"));
            dialog.pack();
            dialog.setVisible(true);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
