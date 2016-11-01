package com.comp445;

import org.apache.commons.cli.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;

import java.util.List;

import static org.apache.http.protocol.HTTP.USER_AGENT;

public class Httpc {

    private String httpcGet(String url, String[] headers, Boolean verbose){

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        // add request header
        request.addHeader("User-Agent", USER_AGENT);

        httpHeaderParser(request,headers);

        return fetchResponse(client, request, url, verbose);
    }

    private String httpcPost(String url, String[] header, String data, String filePath, Boolean verbose){
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);

        //add header
        post.setHeader("User-Agent", USER_AGENT);


        httpHeaderParser(post, header);

        if(data != null && filePath == null){
            try {
                   StringEntity se = new StringEntity(data);
                   post.setEntity(se);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return fetchResponse(client, post, url, verbose);

    }

    private String httpcFilePost(String url, String[] header, String data, String filePath, Boolean verbose){
        Socket client = null;
        try {
            client = new Socket("0.0.0.0",8000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (client != null) {
                OutputStream outputStream = new BufferedOutputStream(client.getOutputStream());
                if(filePath != null && data == null){
                    File file = new File("PostFiles/"+filePath);
                    outputStream.write("POST /".getBytes("UTF-8"));
                    outputStream.write(file.getName().getBytes("UTF-8"));
                    outputStream.write(":".getBytes("UTF-8"));
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int count =0;
                    while((count=fileInputStream.read(buffer)) >= 0){
                        outputStream.write(buffer,0,count);
                    }

                    outputStream.flush();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "httpFilePost";//fetchResponse(client, post, url, verbose);
    }
    

    private String fetchResponse(HttpClient client, HttpRequestBase request, String url, Boolean verbose){
        HttpResponse response = null;
        try {
            response = client.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response != null && verbose) {
            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());
        }

        Header[] resultHeaders = new Header[0];
        if (response != null) {
            resultHeaders = response.getAllHeaders();
        }
        String headerString = "";
        for(Header h: resultHeaders){
            headerString+= h.getName()+" : "+h.getValue()+"\n";
        }

        if(verbose) {
            verboseOption(url, headerString);
        }

        BufferedReader rd = null;
        try {
            if (response != null) {
                rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuffer result = new StringBuffer();
        String line = "";
        try {
            if (rd != null) while ((line = rd.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Output :");
        return result.toString();
    }

    private void httpHeaderParser(HttpRequestBase request, String[] headers){
        if(headers != null) {
            try {
                for (String s : headers) {
                    String prefix = s.substring(0, s.indexOf(":"));
                    String suffix = s.substring(s.indexOf(":")+1, s.length());
                    request.setHeader(prefix, suffix);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void verboseOption(String url, String headerString){
        System.out.println("====================================================");
        System.out.println("Response Header:\n" + headerString.substring(0, headerString.length() - 1));

        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            List<NameValuePair> queryParamsList;
            queryParamsList = uriBuilder.getQueryParams();
            String queryString = "";
            for (NameValuePair nameValuePair : queryParamsList) {
                queryString += nameValuePair.getName() + " : " + nameValuePair.getValue() + "\n";
            }
            System.out.println("====================================================");
            if (!queryString.equals("")) {
                System.out.println("Query Parameters:\n" + queryString.substring(0, queryString.length() - 1));
                System.out.println("====================================================");
            }
        }catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private String generalHelp(){
        return "httpc is a curl-like application but supports HTTP protocol only.\n"
                +"Usage:\n"
                +"\thttpc command [arguments]\n"
                +"The commands are:\n"
                +"\tget     executes a HTTP GET request and prints the response.\n"
                +"\tpost    executes a HTTP POST request and prints the response.\n"
                +"\thelp    prints this screen.\n"
                +"Use \"httpc help [command]\" for more information about a command.";
    }

    private String getHelp(){
        return "usage: httpc get [-v] [-h key:value] URL\n"
                +"Get executes a HTTP GET request for a given URL.\n\n"
                +"\t-v              Prints the detail of the response such as protocol, status, and headers.\n"
                +"\t-h key:value    Associates headers to HTTP Request with the format 'key:value'.";
    }

    private String postHelp(){
        return "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n"
                +"Post executes a HTTP POST request for a given URL with inline data or from file.\n\n"
                +"\t-v             Prints the detail of the response such as protocol, status,and headers.\n"
                +"\t-h key:value   Associates headers to HTTP Request with the format'key:value'.\n"
                +"\t-d string      Associates an inline data to the body HTTP POST request.\n"
                +"\t-f file        Associates the content of a file to the body HTTP POST request.\n\n"
                +"Either [-d] or [-f] can be used but not both.";
    }

    public static void main(String[] args){
		try {
            Options opt = new Options();

            opt.addOption("help", false, "Httpc Help tips");
            opt.addOption("get", false, "Call get function");
            opt.addOption("post", false, "Call Post function");
            opt.addOption("v", false, "Verbose option");
            opt.addOption(OptionBuilder.withLongOpt("h")
                                        .withDescription("Associate Header")
                                        .hasArgs(100)
                                        .create());
            opt.addOption(OptionBuilder.withLongOpt("d")
                            .withDescription("Associate Data")
                            .hasArgs(100)
                            .create());
            opt.addOption("f", true, "File option");

            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse(opt, args);

            if ( cl.hasOption("help")
                    && !cl.hasOption("get")
                    && !cl.hasOption("post") ) {

                System.out.print(new Httpc().generalHelp()); //General Help

            }else if(cl.hasOption("help") && cl.hasOption("get")){

                System.out.print(new Httpc().getHelp());    //GET Help

            }else if(cl.hasOption("help") && cl.hasOption("post")){

                System.out.print(new Httpc().postHelp());    //POST Help

            }else if(cl.hasOption("get")){       //GET Function

                System.out.print(new Httpc().httpcGet(args[args.length-1],cl.getOptionValues('h'),cl.hasOption('v')));

            }
            else if(cl.hasOption("post")){    //POST Function
                if(cl.hasOption('d') && !cl.hasOption('f')) {
                    System.out.print(new Httpc().httpcPost(args[args.length - 1], cl.getOptionValues('h'), cl.getOptionValue('d'), cl.getOptionValue('f'), cl.hasOption('v')));
                }else if(!cl.hasOption('d') && cl.hasOption('f')){
                    System.out.print(new Httpc().httpcFilePost(args[args.length - 1], cl.getOptionValues('h'), cl.getOptionValue('d'), cl.getOptionValue('f'), cl.hasOption('v')));
                }else{
                    System.out.print(new Httpc().postHelp());
                }
            }else{

                System.out.print(new Httpc().generalHelp());
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
