package tester;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class DownloadByHttp {

	private static final String buildServerURLPrefix = "https://snjgsa.xxx.com/projects/t/tsmc/test/fastback/v";
	private static final String filenamePrefix = "fstbk";
	private String buildServerUrl2Folder;
	private String hostName;
	private String loginUser;
	private String loginPass;
	private String releaseNumber;
	private String buildNumber;
	private String platform ;

	// The internal var for storing buildFolder name, like 
	private String buildFolderName;

	/**
	 * 
	 * @param releaseNumber Release number, like 6181.
	 * @param buildNumber	The build number you want to donwload, like 132. Use null if you want to download the latest build.
	 * @param loginUser
	 * @param loginPass
	 */
	public DownloadByHttp(String platform, String releaseNumber, String buildNumber, String loginUser, String loginPass) {
		this.platform = platform;
		this.loginUser = loginUser;
		this.loginPass = loginPass;
		this.releaseNumber = releaseNumber;
		this.hostName = buildServerURLPrefix.split("/")[2];
		this.buildNumber = buildNumber;
	}

	public String getLoginUser() {
		return loginUser;
	}

	public void setLoginUser(String loginUser) {
		this.loginUser = loginUser;
	}

	public String getLoginPass() {
		return loginPass;
	}

	public void setLoginPass(String loginPass) {
		this.loginPass = loginPass;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	private static HttpClient wrapClient(HttpClient base) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
				public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("https", 443, ssf));
			PoolingClientConnectionManager mgr = new PoolingClientConnectionManager(registry);
			return new DefaultHttpClient(mgr, base.getParams());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Get file name from a String that contains the filename.
	 * Like this string:
	 * <img src="/icons/folder.gif" alt="[DIR]"> <a href="6181102_12Aug13/">6181102_12Aug13/</a>        12-Aug-2013 20:54    - 
	 * @param fileNameLine
	 * @return
	 */
	private String getBuildFolderName(String fileNameLine) {
		String fileName = null;
		String[] tmpFileName = fileNameLine.split(">");

		fileName = tmpFileName[2].split("/")[0];

		return fileName;
	}

	/**
	 * Get the folder HTML contents from the remote build server.
	 * @param str
	 * @return
	 */
	private String[] getDirContents() {
		String[] contents = null;
		HttpClient httpclient = wrapClient(new DefaultHttpClient());
		try {
			((AbstractHttpClient) httpclient).getCredentialsProvider().setCredentials(
					new AuthScope(this.hostName, 443),
					new UsernamePasswordCredentials(loginUser, loginPass));
			buildServerUrl2Folder = buildServerURLPrefix + releaseNumber.substring(0, 3) + "/" + platform + "/";

			HttpGet httpget = new HttpGet(buildServerUrl2Folder);

			System.out.println("executing request" + httpget.getRequestLine());
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			System.out.println("----------------------------------------");
			System.out.println(response.getStatusLine());

//			if (entity != null) {
//				System.out.println("Response content length: " + entity.getContentLength());
//			}

			//            String s =  EntityUtils.toString(entity);
			contents = EntityUtils.toString(entity).split("\\n");

		} catch (Exception ex) {
			System.err.println("Error when getting build URL.");
			ex.printStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();
		}
		return contents;
	}

	/**
	 * Get the string that contains certain build. The contents like below:
	 * <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
	 * <html>
	 *  <head>
	 *   <title>Index of /FastBack/pkg/fstbk618</title>
	 *  </head>
	 *  <body>
	 * <h1>Index of /FastBack/pkg/fstbk618</h1>
	 * <pre><img src="/icons/blank.gif" alt="Icon "> <a href="?C=N;O=D">Name</a>                    <a href="?C=M;O=A">Last modified</a>      <a href="?C=S;O=A">Size</a>  <a href="?C=D;O=A">Description</a><hr><img src="/icons/back.gif" alt="[DIR]"> <a href="/FastBack/pkg/">Parent Directory</a>                             -   
	 * <img src="/icons/folder.gif" alt="[DIR]"> <a href="6180149_08Aug13/">6180149_08Aug13/</a>        08-Aug-2013 21:17    -   
	 * <img src="/icons/folder.gif" alt="[DIR]"> <a href="6180150_11Aug13/">6180150_11Aug13/</a>        11-Aug-2013 20:48    -   
	 * <img src="/icons/folder.gif" alt="[DIR]"> <a href="6181102_12Aug13/">6181102_12Aug13/</a>        12-Aug-2013 20:54    -   
	 * <img src="/icons/unknown.gif" alt="[   ]"> <a href="keepfile">keepfile</a>                21-Feb-2013 06:06   27   
	 * <img src="/icons/folder.gif" alt="[DIR]"> <a href="src/">src/</a>                    21-Feb-2013 06:06    -   
	 * <img src="/icons/folder.gif" alt="[DIR]"> <a href="test/">test/</a>                   18-Mar-2013 11:15    -   
	 * <hr></pre>
	 * <address>xxx_HTTP_Server/8.5.0.0 (Unix) at hrlgpfs.haifa.xxx.com Port 80</address>
	 * </body></html>
	 * @param contents
	 * @return
	 */
	private String getFileNameLine(String[] contents) {
		String fileNameLine = null;
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].contains(releaseNumber + buildNumber)) {
				fileNameLine = contents[i];
				break;
			}
		}

		if (fileNameLine == null) {
			System.out.println("Build " + buildNumber + " not found.");
		}

		return fileNameLine;
	}

	private String getLatestFileNameLine(String[] contents) {
		String regex = ".*\\d{7}_\\d{2}\\w{3}\\d{2}.*";
		String fileNameLine = null;
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].matches(regex)) {
				fileNameLine = contents[i];
			}
		}

		if (fileNameLine == null) {
			System.out.println("Build " + buildNumber + " not found.");
		}

		return fileNameLine;
	}

	public String getBuildFileName() {
		String fileNameLine = (buildNumber == null || buildNumber.isEmpty())
				? getLatestFileNameLine(getDirContents()) : getFileNameLine(getDirContents());
				buildFolderName = getBuildFolderName(fileNameLine);
				String fileNamePrefix = filenamePrefix + this.releaseNumber.substring(0, 3);

				return fileNamePrefix + "_" + buildFolderName + ".zip";
	}

	/**
	 * Get the whole build URL, like:
	 * http://hrlgpfs.haifa.xxx.com/FastBack/pkg/fstbk618/6181123_25Sep13/fstbk618_6181123_25Sep13.zip
	 * @return
	 */
	public String getBuildUrl() {
		String fileName = getBuildFileName();
		// The above line must be impelemented first, and the following class member could be initialized.
		String url = buildServerUrl2Folder + buildFolderName + "/" + fileName;

		System.out.println("The build URL is: " + url);
		return url;
	}

	public boolean downloadBuild(String buildUrl, String saveToPath) {
		HttpClient client = wrapClient(new DefaultHttpClient());

		try {
			System.out.println("Start to download the build file...");

			((AbstractHttpClient) client).getCredentialsProvider().setCredentials(
					new AuthScope(this.hostName, 443),
					new UsernamePasswordCredentials(loginUser, loginPass));

			HttpGet httpGet1 = new HttpGet(buildUrl);
			HttpResponse httpResponse1 = client.execute(httpGet1);

			StatusLine statusLine = httpResponse1.getStatusLine();
			if(statusLine.getStatusCode() == HttpStatus.SC_OK) {
				System.out.print("Creating file: " + saveToPath + "...");
				File buildFile = new File(saveToPath);
				System.out.println("DONE");

				HttpEntity entity = httpResponse1.getEntity();
				FileOutputStream outputStream = new FileOutputStream(buildFile);

				// The total size of the downloaded file:
				long totalSize = entity.getContentLength();
				long currSize = 0;

				InputStream inputStream = entity.getContent();
				byte b[] = new byte[1024];
				int j = 0;
				int step = 0;
				while( (j = inputStream.read(b))!=-1) {
					outputStream.write(b,0,j);
					currSize += j;
					float showResult = currSize / (float) totalSize * 100;

					int actualPercentage = 0;
					if ((actualPercentage = (int) Math.floor(showResult)) > step) {
						step++;
						System.out.print(actualPercentage + "% completed.\r");
					}

				}
				outputStream.flush();
				outputStream.close();

				System.out.println("Download finished.");
				return true;
			}
		} catch (Exception ex) {
			System.err.println("Error when downloading the build file.");
			ex.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();

		}
		return false;
	}

	public static void main(String[] args) {
		// http://hrlgpfs.haifa.xxx.com/FastBack/pkg/fstbk618/6181123_25Sep13/fstbk618_6181123_25Sep13.zip
		//		BuildDownloader bd = new BuildDownloader("6181", "123", "Wangxlbj@cn.xxx.com", "Firewall123!");

		String platform = "WINDOWS";
		String ver = "619";
		String build = "0180";
		String gsaUsername = "Wangxlbj";
		String gsaPassword = "Firewall122!";
		String downloadPath = "C:\\builds\\";
		String downloadFilename = ver + build + ".zip";
		DownloadByHttp bd = new DownloadByHttp(platform, ver, build, gsaUsername, gsaPassword);

//		System.out.println(bd.getBuildFileName());
		String url = bd.getBuildUrl();

		String buildPath = downloadPath + downloadFilename;
		bd.downloadBuild(url, buildPath);

	}



}
