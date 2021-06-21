package com.unibeta.cloudtest.tool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Param;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.config.plugin.elements.ParamConfigServicePlugin;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * Provides mail sending and receiving services.
 * 
 * @author jordan.xue
 */
public class MailManager {

	private static final String PROPERTIES_CLOUDTEST = "cloudtest.";
	private static final String PROPERTIES_CLOUDTEST_MAIL = "cloudtest.mail.";
	
	public static final int MAIL_PRIORITY_HIGHEST = 1;
	public static final int MAIL_PRIORITY_NORMAL = 3;
	public static final int MAIL_PRIORITY_HIGH = 2;

	private static Logger logger = LoggerFactory.getLogger(MailManager.class);

	static ParamConfigServicePlugin configServicePlugin = CloudTestPluginFactory
			.getParamConfigServicePlugin();

	/**
	 * Receives mails
	 * 
	 * @return
	 * @throws Exception
	 */
	public static MailReceiverObject receiveMails() throws Exception {

		MailReceiverObject mailReceiverObject = new MailReceiverObject();

		Message[] messages = null;
		List<MimeMessage> pop3MessagesList = new ArrayList<MimeMessage>();

		Store store = null;
		Folder folder = null;

		try {
			Session session = Session.getInstance(buildMailProperties(),
					new Authenticator() {

						protected PasswordAuthentication getPasswordAuthentication() {

							PasswordAuthentication authentication = null;

							try {
								authentication = new PasswordAuthentication(
										configServicePlugin
												.getMailUserAddress(),
										configServicePlugin
												.getMailUserPassword());
							} catch (Exception e) {
								LoggerFactory.getLogger(this.getClass()).error(
										e.getMessage(), e.getCause());
							}

							return authentication;
						}
					});

			store = session
					.getStore(configServicePlugin.getMailStoreProtocal());

			store.connect(configServicePlugin.getMailPop3Host(),
					configServicePlugin.getMailPop3Port(),
					configServicePlugin.getMailUserAddress(),
					configServicePlugin.getMailUserPassword());
			
			String folderName = configServicePlugin.getMailRobotServiceStoreFolder();
			String maxCount = configServicePlugin.getMailRobotServiceReceiveCount();

			folder = store.getFolder(StringUtils.isBlank(folderName) ? "INBOX" : folderName);
			folder.open(Folder.READ_WRITE);

			if (!StringUtils.isBlank(maxCount) && StringUtils.isNumeric(maxCount)) {
				int messageCount = folder.getMessageCount();
				int start = messageCount - Integer.valueOf(maxCount);
				
				if (start >= 0) {
					messages = folder.getMessages(start+1, messageCount);
				} else {
					messages = folder.getMessages(1, messageCount);
				}
			}else {
				messages = folder.getMessages();
			}
			
			for (int i = 0; i < messages.length; i++) {
				Message msg = messages[i];

				if (msg instanceof MimeMessage) {
					pop3MessagesList.add((MimeMessage) msg);
				}
			}
			
			Collections.reverse(pop3MessagesList);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw e;
		}

		mailReceiverObject.setFolder(folder);
		mailReceiverObject.setStore(store);
		mailReceiverObject.setMessages(pop3MessagesList);

		return mailReceiverObject;

	}

	/**
	 * Sends mail with attachments
	 * 
	 * @param to
	 * @param subject
	 * @param text
	 * @param isHtmlText
	 * @param priority
	 *            1(highest),2(high),3(normal),4(low),5(lowest)
	 * @throws Exception
	 */
	public static void sendMail(String to, String subject, String text,
			List<DataSource> attachDataResources, Boolean isHtmlText,
			Integer priority) throws Exception {

		if (null != to) {
			to = to.trim();
			if (to.endsWith(";")) {
				to = to.substring(0, to.length() - 1);
			}
			if (to.startsWith(";")) {
				to = to.substring(1);
			}
		}

		if (CommonUtils.isNullOrEmpty(to)) {
			return;
		}

		JavaMailSenderImpl senderimpl = new JavaMailSenderImpl();

		String[] mailto = to.split(";");

		senderimpl.setHost(configServicePlugin.getMailHost());
		senderimpl.setUsername(configServicePlugin.getMailUsername());
		senderimpl.setPassword(configServicePlugin.getMailUserPassword());

		senderimpl.setJavaMailProperties(buildMailProperties());

		MimeMessage mailmessage = senderimpl.createMimeMessage();

		MimeMessageHelper messagehelper = new MimeMessageHelper(mailmessage,
				true,"UTF-8");
		messagehelper.setFrom(configServicePlugin.getMailUserAddress());
		messagehelper.setTo(mailto);
		messagehelper.setSubject(subject);
		messagehelper.setText(text, isHtmlText);
		messagehelper.setPriority(priority);

		if (null != attachDataResources) {
			for (DataSource ds : attachDataResources) {
				if (null != ds) {
					messagehelper.addAttachment(ds.getName(), ds);
				}
			}
		}

		senderimpl.send(mailmessage);
		logger.info("email was sent successfully to: "+ to);
	}

	private static Properties buildMailProperties() throws Exception {
		Properties prop = new Properties();

		List<Param> params = PluginConfigProxy.loadGlobalPluginConfig().param;

		for (Param p : params) {
			if (null != p && p.name != null) {
				String key = p.name.toLowerCase();
				if (key.startsWith(PROPERTIES_CLOUDTEST_MAIL)) {
					prop.setProperty(
							key.substring(PROPERTIES_CLOUDTEST.length()), p.value);
				}

			}
		}

		return prop;
	}

	/**
	 * Sends mail with attachments
	 * 
	 * @param to
	 * @param subject
	 * @param text
	 * @param isHtmlText
	 * @param priority
	 * @throws Exception
	 */
	public static void sendMail(String to, String subject, String text,
			Boolean isHtmlText, Integer priority) throws Exception {

		sendMail(to, subject, text, null, isHtmlText, priority);
	}

	public static String getMailContent(Part part, StringBuffer bodyText)
			throws MessagingException, IOException {

		if (null == bodyText) {
			bodyText = new StringBuffer();
		}

		String contentType = part.getContentType();

		int nameindex = contentType.indexOf("name");

		boolean conname = false;

		if (nameindex != -1) {

			conname = true;

		}

		if (part.isMimeType("text/plain") && !conname) {

			bodyText.append((String) part.getContent());

		} else if (part.isMimeType("text/html") && !conname) {

			bodyText.append((String) part.getContent());

		} else if (part.isMimeType("multipart/*")) {

			Multipart multipart = (Multipart) part.getContent();

			int count = multipart.getCount();

			for (int i = 0; i < count; i++) {

				getMailContent(multipart.getBodyPart(i), bodyText);

			}

		} else if (part.isMimeType("message/rfc822")) {

			getMailContent((Part) part.getContent(), bodyText);

		}

		return bodyText.toString();
	}

	public static boolean isContainAttch(Part part) throws MessagingException,
			IOException {

		boolean flag = false;

		if (part.isMimeType("multipart/*")) {

			Multipart multipart = (Multipart) part.getContent();

			int count = multipart.getCount();

			for (int i = 0; i < count; i++) {

				BodyPart bodypart = multipart.getBodyPart(i);

				String dispostion = bodypart.getDisposition();

				if ((dispostion != null)
						&& (dispostion.equals(Part.ATTACHMENT) || dispostion
								.equals(Part.INLINE))) {

					flag = true;

				} else if (bodypart.isMimeType("multipart/*")) {

					flag = isContainAttch(bodypart);

				} else {

					String conType = bodypart.getContentType();

					if (conType.toLowerCase().indexOf("appliaction") != -1) {

						flag = true;

					}

					if (conType.toLowerCase().indexOf("name") != -1) {

						flag = true;

					}

				}

			}

		} else if (part.isMimeType("message/rfc822")) {

			flag = isContainAttch((Part) part.getContent());

		}

		return flag;

	}

	/**
	 * Save mail's attachment to saveAttchPath.
	 * 
	 * @param part
	 * @param saveAttchPath
	 *            relative path. such as d:/temp/
	 * @return saved attachment file's path name
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static List<String> saveAttachment(Part part, String saveAttchPath)
			throws MessagingException, IOException {

		String filename = "";
		List<String> list = new ArrayList<String>();

		if (part.isMimeType("multipart/*")) {

			Multipart mp = (Multipart) part.getContent();

			for (int i = 0; i < mp.getCount(); i++) {

				BodyPart mpart = mp.getBodyPart(i);

				String dispostion = mpart.getDisposition();

				if ((dispostion != null)
						&& (dispostion.equals(Part.ATTACHMENT) || dispostion
								.equals(Part.INLINE))) {

					filename = mpart.getFileName();

					if (filename.toLowerCase().indexOf("gb2312") != -1) {

						filename = MimeUtility.decodeText(filename);

					}

					list.add(saveFile(saveAttchPath, filename,
							mpart.getInputStream()));

				} else if (mpart.isMimeType("multipart/*")) {

					list.addAll(saveAttachment(mpart, saveAttchPath));

				} else {

					filename = mpart.getFileName();

					if (filename != null
							&& (filename.toLowerCase().indexOf("gb2312") != -1)) {

						filename = MimeUtility.decodeText(filename);

					}

					list.add(saveFile(saveAttchPath, filename,
							mpart.getInputStream()));

				}

			}

		} else if (part.isMimeType("message/rfc822")) {

			list.addAll(saveAttachment((Part) part.getContent(), saveAttchPath));

		}

		return list;
	}

	private static String saveFile(String saveAttchPath, String filename,
			InputStream inputStream) throws IOException {

		String storedir = saveAttchPath;
		String sepatror = File.separator;

		File storefile = new File(storedir + sepatror + filename);

		BufferedOutputStream bos = null;

		BufferedInputStream bis = null;

		try {

			bos = new BufferedOutputStream(new FileOutputStream(storefile));

			bis = new BufferedInputStream(inputStream);

			int c;

			while ((c = bis.read()) != -1) {

				bos.write(c);

				bos.flush();

			}

		} catch (FileNotFoundException e) {

		} catch (IOException e) {

			logger.error(CloudTestUtils.printExceptionStackTrace(e));

		} finally {

			bos.close();
			bis.close();

		}

		return storefile.getPath();
	}

	public static class MailReceiverObject {

		Store store = null;
		Folder folder = null;
		List<MimeMessage> messages = null;

		public Store getStore() {

			return store;
		}

		public void setStore(Store store) {

			this.store = store;
		}

		public Folder getFolder() {

			return folder;
		}

		public void setFolder(Folder folder) {

			this.folder = folder;
		}

		public List<MimeMessage> getMessages() {

			return messages;
		}

		public void setMessages(List<MimeMessage> messages) {

			this.messages = messages;
		}

	}
}
