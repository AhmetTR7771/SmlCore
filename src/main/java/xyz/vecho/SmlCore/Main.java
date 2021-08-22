package xyz.vecho.SmlCore;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.jbsdiff.Patch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Main {

	public static void main(String[] args) {
		OptionParser optionparser = new OptionParser();
		optionparser.allowsUnrecognizedOptions();
		optionparser.accepts("downloadjre");
		optionparser.accepts("usejre");
		OptionSpec<String> jarPath = optionparser.accepts("jarpath").withRequiredArg().required();
		OptionSpec<String> userNameSpec = optionparser.accepts("username").withOptionalArg().required();
		OptionSpec<String> versionSpec = optionparser.accepts("version").withOptionalArg().required();
		OptionSet optionset = optionparser.parse(args);
		File file = new File(optionset.valueOf(jarPath));
		try {
			if (!file.exists()) copy(connect("https://vecho.cf/api/sml/originalJar.jar"), new FileOutputStream(file));
			JsonObject object = JsonParser.parseReader(new InputStreamReader(connect("https://vecho.cf/api/sml/lastupdate.php"))).getAsJsonObject();
			boolean isUp = false;
			if (object.get("patchedJar").getAsString().equalsIgnoreCase(sha1(file))) isUp = true; // already updated
			
			File patchFile = new File(file.getParentFile(), "sml.patch");
			if (patchFile.exists()) patchFile.delete();
			
			if (!isUp) {
				if (!object.get("originalJar").getAsString().equalsIgnoreCase(sha1(file))) {
					file.delete();
					copy(connect("https://vecho.cf/api/sml/originalJar.jar"), new FileOutputStream(file));
				}
				
				copy(connect("https://vecho.cf/api/sml/sml.patch"), new FileOutputStream(patchFile));
				
				byte[] originalBytes = Files.readAllBytes(file.toPath());
				byte[] patch = readFully(new FileInputStream(patchFile));
	            final OutputStream jarOutput = new BufferedOutputStream(new FileOutputStream(file));
				
	            Patch.patch(originalBytes, patch, jarOutput);
	            
	            if (!sha1(file).equalsIgnoreCase(object.get("patchedJar").getAsString())) {
	            	System.out.println("Cannot patch simpleminecraftlauncher");
	            	System.exit(1);
	            } else System.out.println("Update successful");
	            
	            patchFile.delete();
			}
            
            List<String> cmds = new ArrayList<String>();
            
            cmds.add("javaw");
            
            cmds.add("-jar");
            
            cmds.add("\""+file.getAbsolutePath()+"\"");
            
            boolean flag = optionset.has("downloadjre") || optionset.has("usejre");
            if (flag) cmds.add("-usejre");
            
            cmds.add("-username");
            
            cmds.add(optionset.valueOf(userNameSpec));
            
            cmds.add("-version");
            
            cmds.add(optionset.valueOf(versionSpec));
            
            cmds.add("-skipupdate");
            
            Process p = Runtime.getRuntime().exec(cmds.toArray(new String[0]));
            
			while (p.isAlive()) {
				copy(p.getInputStream(), System.out);
				copy(p.getErrorStream(), System.err);
			}
		} catch (Exception e) {
			System.out.println("An error caught!");
			e.printStackTrace();
		}
	}

    public static void copy(InputStream in, OutputStream out) throws IOException {
		int n;
		byte[] buffer = new byte[8192]; 
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
		out.close();
    }
	
	public static InputStream connect(String url) throws IOException {
		final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		conn.connect();
		return conn.getInputStream();
	}
	
	public static String sha1(File file) throws Exception {
		if (!file.exists()) return "";
		byte[] fileBytes = Files.readAllBytes(file.toPath());
		byte[] hash = MessageDigest.getInstance("sha-1").digest(fileBytes);
		return DatatypeConverter.printHexBinary(hash);
	}
	
    private static byte[] readFully(final InputStream in) throws IOException {
        try {
            byte[] buffer = new byte[16 * 1024];
            int off = 0;
            int read;
            while ((read = in.read(buffer, off, buffer.length - off)) != -1) {
                off += read;
                if (off == buffer.length) {
                    buffer = Arrays.copyOf(buffer, buffer.length * 2);
                }
            }
            return Arrays.copyOfRange(buffer, 0, off);
        } finally {
            in.close();
        }
    }
}
