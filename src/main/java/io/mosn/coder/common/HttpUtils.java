package io.mosn.coder.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * @author yiji@apache.org
 */
public class HttpUtils {

    public static String http(String requestURL, String method, String json) {
        HttpURLConnection conn = null;
        String result = "";
        DataOutputStream out = null;
        try {
            java.net.URL url = URI.create(requestURL).toURL();
            conn = createConnection(url, method, true, true);

            /**
             * connection failed
             */
            if (conn == null) return null;

            if (json != null && json.length() > 0) {
                out = new DataOutputStream(conn.getOutputStream());
                out.write(json.getBytes());
                out.flush();
            }
            result = readAll(conn);
        } catch (Exception e) {
            return result;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return result;
    }

    private static HttpURLConnection createConnection(URL url, String method, boolean doOutput, boolean doInput) {
        HttpURLConnection con = null;

        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setDoOutput(doOutput);
            con.setDoInput(doInput);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "text/plain");
            return con;
        } catch (IOException e) {
            return con;
        }
    }

    private static String readAll(HttpURLConnection conn) throws IOException {
        String result = "";
        BufferedReader buf = null;
        try {
            int code = conn.getResponseCode();
            if (code == 200) {
                StringBuilder buffer = new StringBuilder();
                buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String temp;
                while ((temp = buf.readLine()) != null) {
                    buffer.append(temp);
                    buffer.append("\n");
                }
                result = buffer.toString().trim();
            } else {
            }
        } catch (IOException e) {
        } finally {
            if (buf != null) {
                buf.close();
            }
        }

        return result;
    }

}
