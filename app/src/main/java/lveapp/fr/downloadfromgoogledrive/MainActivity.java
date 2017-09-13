package lveapp.fr.downloadfromgoogledrive;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private EditText shareLink;
    private EditText fileName;
    private Button downloadFile;

    private static final String COOKIES_HEADER = "Set-Cookie";
    private static java.net.CookieManager msCookieManager = new java.net.CookieManager();

    private DownloadHtmlFile htmlFile;

    private static String FOLDER_NAME = "Google-drive-folder";

    private static String MY_FILE_NAME;

    private static String FILE_ID;
    private static final String DIRECT_LINK = "https://drive.google.com/uc?export=download&id=";
    private static String DOWNLOAD_DIRECT_LINK = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        shareLink = (EditText)findViewById(R.id.shareLink);
        fileName = (EditText)findViewById(R.id.fileName);

        downloadFile = (Button)findViewById(R.id.downloadFile);
        downloadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Field url entered
                String mUrlDownload = shareLink.getText().toString().trim();
                if(mUrlDownload.length() == 0){
                    shareLink.setError(getResources().getString(R.string.lb_field_requires));
                    return;
                }
                // File ID
                String fileID = getFileIDFromGoogleLink(mUrlDownload);
                Log.i("TAG_FILE_ID", fileID);
                if(!fileID.equalsIgnoreCase("")){
                    FILE_ID = fileID;
                }
                else{
                    shareLink.setError(getResources().getString(R.string.lb_field_invalidate));
                    return;
                }
                // Field fileName entered
                String mFileName = fileName.getText().toString().trim();
                if(mFileName.length() == 0){
                    fileName.setError(getResources().getString(R.string.lb_field_requires));
                    return;
                }
                else{
                    MY_FILE_NAME = mFileName;
                }
                // Internet connection
                if(isConnexionExiste()){
                    // Permission to save file
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                        }
                        else {
                            // If permission accepted
                            htmlFile = new DownloadHtmlFile();
                            htmlFile.execute();
                        }
                    }
                    else {
                        // Permission accepted
                        htmlFile = new DownloadHtmlFile();
                        htmlFile.execute();
                    }
                }
                else{
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.lb_connection_requires), Toast.LENGTH_LONG).show();
                }

            }
        });

        // Launch Download Service
        Intent downloadIntent = new Intent(MainActivity.this, DownloadService.class);
        startService(downloadIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            // Permission To save file
            case 2:
                htmlFile = new DownloadHtmlFile();
                htmlFile.execute();
                break;
        }
    }

    private static String getIntroVideoLocalPath(){
        File SDCardRoot = Environment.getExternalStorageDirectory();
        return Environment.getExternalStoragePublicDirectory(SDCardRoot + "/" + FOLDER_NAME + "/" + MY_FILE_NAME).getAbsolutePath();
    }


    private static void getFileByDownloadManager(Context context){
        boolean authoriseDownload = false;
        File SDCardRoot = Environment.getExternalStorageDirectory();
        String fileLocalPath = Environment.getExternalStoragePublicDirectory(SDCardRoot + "/" + FOLDER_NAME + "/" + MY_FILE_NAME).getAbsolutePath();
        // Verify and Delete if file exists
        File oldFile = new File(fileLocalPath);
        if(oldFile.exists()){
            oldFile.delete();
            authoriseDownload = true;
        }
        else{
            authoriseDownload = true;
        }
        //--
        if(authoriseDownload){
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DOWNLOAD_DIRECT_LINK));
                request.setTitle(FOLDER_NAME);
                request.setDescription(FOLDER_NAME);
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                request.setAllowedOverRoaming(false);

                if (null != msCookieManager && msCookieManager.getCookieStore().getCookies().size() > 0) {
                    request.addRequestHeader("Cookie", TextUtils.join(";",  msCookieManager.getCookieStore().getCookies()));
                    Log.i("TAG_COOKIE_VALUE", TextUtils.join(";",  msCookieManager.getCookieStore().getCookies()));
                }

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                // Create folder
                createFolder();
                // Set destination of file
                request.setDestinationInExternalPublicDir(SDCardRoot + "/" + FOLDER_NAME, MY_FILE_NAME);
                // Download
                DownloadManager manager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
            }
            catch (Exception ex){}
        }
    }


    private static void createFolder(){
        try {
            File root = new File(Environment.getExternalStorageDirectory(), FOLDER_NAME);
            if (!root.exists()) {
                root.mkdirs();
            }
        }
        catch (Exception e){
            Log.i("TAG_FOLDER_ERROR", e.getMessage());
        }
    }

    public static ArrayList<String> extractUrls(String text)
    {
        ArrayList<String> containedUrls = new ArrayList<>();
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find())
        {
            containedUrls.add(text.substring(urlMatcher.start(0), urlMatcher.end(0)));
        }

        return containedUrls;
    }

    private class DownloadHtmlFile extends AsyncTask<Void, Void, String> {

        HttpURLConnection urlConnection;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder responseHttp = new StringBuilder();
            try {
                URL url = new URL(DIRECT_LINK+FILE_ID);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    responseHttp.append(line.replace("uc?export=download", "https://drive.google.com/uc?export=download"));
                }

            }catch( Exception e) {
                e.printStackTrace();
            }
            finally {
                // Disconnect
                urlConnection.disconnect();
            }
            //--
            ArrayList<String> resultatTMP = extractUrls(responseHttp.toString());
            for (String resultat : resultatTMP){
                if(resultat.contains("uc?export=download")){
                    return resultat.replace("amp;", "");
                }
            }
            //--
            return null;
        }

        @Override
        protected void onPostExecute(String resultat) {
            super.onPostExecute(resultat);
            //--
            if(resultat != null){
                DOWNLOAD_DIRECT_LINK = resultat;

                // Get Cookies from response header and load them into cookieManager
                Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
                List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
                if (cookiesHeader != null) {
                    for (String cookie : cookiesHeader) {
                        msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                    }
                }
            }
            else{
                DOWNLOAD_DIRECT_LINK = DIRECT_LINK + FILE_ID;
            }

            progressBar.setVisibility(View.GONE);

            Toast.makeText(MainActivity.this, getResources().getString(R.string.lb_downloading), Toast.LENGTH_LONG).show();

            getFileByDownloadManager(MainActivity.this);
            Log.i("TAG_DIRECT_LINK", DOWNLOAD_DIRECT_LINK);
        }
    }

    private String getFileIDFromGoogleLink(String linkOrID){
        String resultat = null;
        if(linkOrID.contains("https://drive.google.com/open?id=")){
            if(linkOrID.split("id=").length==2){
                resultat = linkOrID.replace("https://drive.google.com/open?id=", "");
            }
        }
        else if(linkOrID.contains("https://drive.google.com/file/d/") && linkOrID.contains("/view")){
            String resultTmp = linkOrID.replace("https://drive.google.com/file/d/", "");
            resultat = resultTmp.split("/view")[0];
        }
        else if(!linkOrID.contains("/")){
            resultat = linkOrID;
        }
        return resultat;
    }

    private String getFileNameWithoutExtension(String filename){
        String resultat = "";
        String tabResultat[] = filename.split(".");
        if(tabResultat.length >= 2){
            for(int i=0; i<tabResultat.length-1; i++){
                resultat += tabResultat[i];
            }
        }
        return sansAccent(resultat.replace(" ", "-"));
    }

    private String getFileExtension(String filename){
        String resultat = "";
        String tabResultat[] = filename.split(".");
        if(tabResultat.length >= 2){
            resultat = tabResultat[tabResultat.length-1];
        }
        return "."+resultat;
    }


    private String sansAccent(String src){
        String resultat = Normalizer.normalize(src, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        return resultat.replaceAll("\\W","_");
    }

    /**
     * Ne pas oublier dans le fichier manifeste : android.permission.ACCESS_NETWORK_STATE
     * */
    private boolean isConnexionExiste() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
