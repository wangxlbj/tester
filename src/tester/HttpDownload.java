package tester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * ˵�� ����httpclient�����ļ�
 * 
 * <dependency> <groupId>org.apache.httpcomponents</groupId>
 * <artifactId>httpclient</artifactId> <version>4.0.1</version> </dependency>
 * ������http�ļ���ͼƬ��ѹ���ļ� bug����ȡresponse header��Content-Disposition��filename������������
 * 
 * @author anonymous
 * 
 */

public class HttpDownload {

	public static final boolean isWindows;
	public static final String splash;
	public static final String root;
	static {
		if (System.getProperty("os.name") != null && System.getProperty("os.name").toLowerCase().contains("windows")) {
			isWindows = true;
			splash = "\\";
			root = "D:";
		} else {
			isWindows = false;
			splash = "/";
			root = "/search";
		}
	}

	/**
	 * ����url�����ļ����ļ�����response headerͷ�л�ȡ
	 * 
	 * @param url
	 * @return
	 */
	public static String download(String url) {
		return download(url, null);
	}

	/**
	 * ����url�����ļ������浽filepath��
	 * 
	 * @param url
	 * @param filepath
	 * @return
	 */
	public static String download(String url, String savetopath) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = client.execute(httpget);

			HttpEntity entity = response.getEntity();
			InputStream inputstream = entity.getContent();

			long length = entity.getContentLength();
			if (length <= 0) {
				System.out.println("�����ļ������ڣ�");
				return null;
			}

			System.out.println("The response value of token:" + response.getFirstHeader("token"));

			if (savetopath == null)
				savetopath = getFilePath(response);
			File file = new File(savetopath);
			file.getParentFile().mkdirs();
			FileOutputStream fileout = new FileOutputStream(file);
			byte buf[] = new byte[1024];
			int l = 0;
			while ((l = inputstream.read(buf)) != -1) {
				fileout.write(buf, 0, l); // ע�����������OutputStream.write(buff)�Ļ���ͼƬ��ʧ�棬��ҿ�������
			}
			inputstream.close();
			fileout.flush();
			fileout.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ���������ļ��Ĵ洢·��
	 * 
	 * @param response
	 * @return
	 */
	public static String getFilePath(HttpResponse response) {
		String savetopath = root + splash;
		String filename = getFileName(response);

		if (filename != null) {
			savetopath += filename;
		} else {
			savetopath += getRandomFileName();
		}
		return savetopath;
	}

	/**
	 * ��ȡresponse header��Content-Disposition�е�filenameֵ
	 * 
	 * @param response
	 * @return
	 */
	public static String getFileName(HttpResponse response) {
		Header contentHeader = response.getFirstHeader("Content-Disposition");
		String filename = null;
		if (contentHeader != null) {
			HeaderElement[] values = contentHeader.getElements();
			if (values.length == 1) {
				NameValuePair param = values[0].getParameterByName("filename");
				if (param != null) {
					try {
						// filename = new
						// String(param.getValue().toString().getBytes(),
						// "utf-8");
						// filename=URLDecoder.decode(param.getValue(),"utf-8");
						filename = param.getValue();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return filename;
	}

	/**
	 * ��ȡ����ļ���
	 * 
	 * @return
	 */
	public static String getRandomFileName() {
		return String.valueOf(System.currentTimeMillis());
	}

	// public static void outHeaders(HttpResponse response) {
	// Header[] headers = response.getAllHeaders();
	// for (int i = 0; i < headers.length; i++) {
	// System.out.println(headers[i]);
	// }
	// }
	public static void main(String[] args) {
		// String url =
		// "http://bbs.btwuji.com/job.php?action=download&pid=tpc&tid=320678&aid=216617";
		String url = "http://s3.sinaimg.cn/bmiddle/5f4e2f78g9c4b6f1d5a62&690";
		// String filepath = "D:\\test\\a.torrent";
		String filepath = "D:\\test\\a.jpg";
		HttpDownload.download(url, filepath);
	}
}
