import java.io.FileInputStream;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 发邮件的实质就是在一个 Session 中通过：transport 发 MimeMessage <br/>
 * 就好像在一个通话过程中，通过一个邮递员，发送一封信息 <br/>
 * 所以我们需要 <br/>
 * 1. 先打开一个Session <br/>
 * 2. 在Session中初始化好用MimeMessage <br/>
 * 3. 在Session中打开transport, 用transport把MimeMessage发出去
 * 
 * @author wangguozheng
 * 
 */
public class SendEmail {
	private final static Logger logger = LoggerFactory.getLogger(SendEmail.class);
	// 0. 初始化一个常量 properties文件PP
	private final static Properties PP;
	static {
		PP = new Properties();
		try {
			//用getResource方法获得 p.xml 的相对路径
			//经过编译之后SendEmail.class文件和p.xml文件在一个文件夹内
			//getResource会找在SendEmail.class文件夹内是否由p.xml，找到的话会议URL形式返回其地址
			//转化成URI是因为如果地址中由空格，URL会自动转化成%20，一会儿用来获得文件的时候就不识别了
			String filePath = SendEmail.class.getResource("p.xml").toURI().getPath();
			logger.debug(filePath);
			
			//用XML文件有一个好处：可以有多行的内容, 而且搭配合适的CDATA，也不用担心 "<" ">"这种字符的转义
			PP.loadFromXML(new FileInputStream(filePath));
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.debug("property 文件载入成功");
		System.exit(0);
	}

	public static void main(String[] args) throws Exception {
		// 1. 通过必要的参数创建一个Session[Session需要的参数直接从Property文件中获得]
		// 不要用getDefaultInstance 而用 getInstance
		// 参考: http://www.oracle.com/technetwork/java/javamail/faq/index.html#commonmistakes
		Session session = Session.getInstance(PP);

		// 2. 通过必要的参数+Session初始化一个MimeMessage
		MimeMessage message = initMsg(session, PP);

		// 3. 通过Session创建一个transport，要明确采用的协议是什么
		Transport transport = session.getTransport(PP.getProperty("mail.sender.protocol"));

		// 4. 用transport+（发邮件服务器的用户名和密码）建立链接
		// 网上很多代码使用Authenticator绑定用户名和密码，这种方法并不推荐
		// 参考 http://www.oracle.com/technetwork/java/javamail/faq/index.html#commonmistakes
		transport.connect(PP.getProperty("mail.sender.name"), PP.getProperty("mail.sender.pass"));

		// 5. 用transport把刚才初始化的message发出去
		transport.sendMessage(message, message.getAllRecipients());
		
		logger.debug("成功发送邮件");
	}

	/**
	 * 这个方法利用一个Session对象，通过propety文件里的信息，初始化一个MimeMessage
	 * 
	 * @param session
	 * @param p
	 * @return MimeMessage
	 * @throws Exception
	 */
	public static MimeMessage initMsg(Session session, Properties p) throws Exception {
		// 1. 通过Session对象获得MimeMessage
		MimeMessage message = new MimeMessage(session);
		// 2. 明确发件方 邮件地址
		message.setFrom(new InternetAddress(p.getProperty("mail.sender")));
		// 2. 明确收件方 邮件地址
		Scanner sc = new Scanner(p.getProperty("mail.receiver"));
		// 3. 循环假如所有的收件人地址
		while (sc.hasNext()) {
			String line = sc.next();
			if (line.trim().isEmpty()) {
				continue;
			}
			logger.debug("增加了收件方:" + line);
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(line));
		}
		sc.close();
		// 4. 明确邮件 主题
		message.setSubject(p.getProperty("mail.subject"));
		// 5. 明确邮件 内容
		message.setText(p.getProperty("mail.txt"));
		return message;
	}
}