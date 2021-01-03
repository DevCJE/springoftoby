package springbook.user.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static springbook.user.service.UserServiceImpl.MIN_LOGCOUNT_FOR_SILVER;
import static springbook.user.service.UserServiceImpl.MIN_RECCOMEND_FOR_GOLD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import springbook.user.dao.UserDao;
import springbook.user.domain.Level;
import springbook.user.domain.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/test-applicationContext.xml")
public class UserServiceTest {

	@Autowired UserService userService;
	// 같은 타입의 빈이 두 개 존재하기 때문에 필드의 이름을 기준으로 주입될 빈이 결정된다.
	// 자동 프록시 생성기에 의해 트랜잭션 부가기능이 제대로 적용되었는지 확인하는 것이 목적이다.
	@Autowired UserService testUserService;
	@Autowired UserDao userDao;
	@Autowired MailSender mailSender;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired ApplicationContext context;
	
	List<User> users;	// test fixture

	@Before
	public void setUp() {
		users = Arrays.asList(
				new User("bumjin", "박범진", "p1", "user1@ksug.org", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER-1, 0),
				new User("joytouch", "강명성", "p2", "user2@ksug.org", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0),
				new User("erwins", "신승한", "p3", "user3@ksug.org", Level.SILVER, 60, MIN_RECCOMEND_FOR_GOLD-1),
				new User("madnite1", "이상호", "p4", "user4@ksug.org", Level.SILVER, 60, MIN_RECCOMEND_FOR_GOLD),
				new User("green", "오민규", "p5", "user5@ksug.org", Level.GOLD, 100, Integer.MAX_VALUE)
				);
	}

	@Test 
	public void upgradeLevels() throws Exception {
		// 고립된 테스트에서는 테스트 대상 오브젝트를 직접 생성하면 된다.
		UserServiceImpl userServiceImpl = new UserServiceImpl();

		// 목 오브젝트로 만든 UserDao를 직접 DI 해준다.
		MockUserDao mockUserDao = new MockUserDao(this.users);  
		userServiceImpl.setUserDao(mockUserDao);

		// 메일 발송 여부 확인을 위해 목 오브젝트 DI
		MockMailSender mockMailSender = new MockMailSender();
		userServiceImpl.setMailSender(mockMailSender);

		// 테스트 대상 실행
		userServiceImpl.upgradeLevels();

		List<User> updated = mockUserDao.getUpdated();  
		assertThat(updated.size(), is(2));  
		checkUserAndLevel(updated.get(0), "joytouch", Level.SILVER); 
		checkUserAndLevel(updated.get(1), "madnite1", Level.GOLD);

		// 목 오브젝트를 이용한 결과 확인.
		// 목 오브젝트에 저장된 메일 수신자 목록을 가져와 업그레이드 대상과 일치하는지 확인 한다.
		List<String> request = mockMailSender.getRequests();
		assertThat(request.size(), is(2));
		assertThat(request.get(0), is(users.get(1).getEmail()));
		assertThat(request.get(1), is(users.get(3).getEmail()));
	}

	private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
		assertThat(updated.getId(), is(expectedId));
		assertThat(updated.getLevel(), is(expectedLevel));
	}
	
	static class MockUserDao implements UserDao { 
		private List<User> users;  
		private List<User> updated = new ArrayList(); 
		
		private MockUserDao(List<User> users) {
			this.users = users;
		}

		public List<User> getUpdated() {
			return this.updated;
		}

		public List<User> getAll() {  
			return this.users;
		}

		public void update(User user) {  
			updated.add(user);
		}
		
		public void add(User user) { throw new UnsupportedOperationException(); }
		public void deleteAll() { throw new UnsupportedOperationException(); }
		public User get(String id) { throw new UnsupportedOperationException(); }
		public int getCount() { throw new UnsupportedOperationException(); }
	}
	
	static class MockMailSender implements MailSender {
		private List<String> requests = new ArrayList<String>();	
		
		public List<String> getRequests() {
			return requests;
		}

		public void send(SimpleMailMessage mailMessage) throws MailException {
			requests.add(mailMessage.getTo()[0]);  
		}

		public void send(SimpleMailMessage[] mailMessage) throws MailException {
		}
	}
	
	@Test
	public void mockUpgradeLevels() throws Exception {
		UserServiceImpl userServiceImpl = new UserServiceImpl();

		// 다이나믹한 목 오브젝트 생성과 메소드의 리턴 값 설정, 그리고 DI까지 세 줄이면 충분하다.
		UserDao mockUserDao = mock(UserDao.class);	    
		when(mockUserDao.getAll()).thenReturn(this.users);
		userServiceImpl.setUserDao(mockUserDao);

		// 리턴 값이 없는 메소드를 가진 목 오브젝트는 더욱 간단하게 만들 수 있다.
		MailSender mockMailSender = mock(MailSender.class);  
		userServiceImpl.setMailSender(mockMailSender);

		userServiceImpl.upgradeLevels();

		// 목 오브젝트가 제공하는 검증 기능을 통해서 어떤 메소드가 몇 번 호출 됐는지, 파라미터는 무엇인지 확인할 수 있다.
		verify(mockUserDao, times(2)).update(any(User.class));				  
		verify(mockUserDao, times(2)).update(any(User.class));
		verify(mockUserDao).update(users.get(1));
		assertThat(users.get(1).getLevel(), is(Level.SILVER));
		verify(mockUserDao).update(users.get(3));
		assertThat(users.get(3).getLevel(), is(Level.GOLD));

		// Mockito 프레임워크의 툴 사용법으로써 배우지 않은 부분이니 그냥 보내질 메일 주소를 확인한다고 알고 있자.
		ArgumentCaptor<SimpleMailMessage> mailMessageArg = ArgumentCaptor.forClass(SimpleMailMessage.class);  
		verify(mockMailSender, times(2)).send(mailMessageArg.capture());
		List<SimpleMailMessage> mailMessages = mailMessageArg.getAllValues();
		assertThat(mailMessages.get(0).getTo()[0], is(users.get(1).getEmail()));
		assertThat(mailMessages.get(1).getTo()[0], is(users.get(3).getEmail()));
	}	

	private void checkLevelUpgraded(User user, boolean upgraded) {
		User userUpdate = userDao.get(user.getId());
		if (upgraded) {
			assertThat(userUpdate.getLevel(), is(user.getLevel().nextLevel()));
		}
		else {
			assertThat(userUpdate.getLevel(), is(user.getLevel()));
		}
	}

	@Test 
	public void add() {
		userDao.deleteAll();
		
		User userWithLevel = users.get(4);	  // Level.GOLD 상태
		User userWithoutLevel = users.get(0);  
		userWithoutLevel.setLevel(null);	  // Level을 null로 초기화
		
		userService.add(userWithLevel);	  
		userService.add(userWithoutLevel);

		// 저장된 정보를 read 하여 객체 생성
		User userWithLevelRead = userDao.get(userWithLevel.getId());
		User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());
		
		assertThat(userWithLevelRead.getLevel(), is(userWithLevel.getLevel())); 
		assertThat(userWithoutLevelRead.getLevel(), is(Level.BASIC));
	}
 
	@Test
	public void upgradeAllOrNothing() throws Exception {

		/* 해당 부분은 이제 ProxyFactoryBean을 이용하지 않고
		빈 후처리기 생성기인 DefaultAdvisorAutoProxyCreator를 이용하기 때문에
		아래와 같이 사용할 필요가 없어졌다.

		// 팩토리 빈 자체를 가져와야 하므로 빈 이름에 & 를 반드시 넣어야 한다.
		// getBean()의 처음 패러미터는 우리가 @Bean으로 설정한 메소드의 이름이며 getBean()의 리턴타입은 Object인데
		// 이를 두번째 패러미터를 이용하여서 캐스팅을 하지 않고 바로 리턴 타입을 설정할 수 있다.
		ProxyFactoryBean txProxyFactoryBean =
			context.getBean("&userService", ProxyFactoryBean.class);
		txProxyFactoryBean.setTarget(testUserService); // 테스트용 타깃 주입
		// 변경된 타깃 설정을 이용해서 트랜잭션 다이내믹 프록시 오브젝트를 다시 생성한다.
		UserService txUserService = (UserService) txProxyFactoryBean.getObject();

        기존 트랜잭션을 넣기 위해서 만들었던 데코레이터 패턴을 이용한 프록시이나 다이내믹 프록시에 의해 대체되었다.
        UserServiceTx txUserService = new UserServiceTx();
        txUserService.setTransactionManager(transactionManager);
        txUserService.setUserService(testUserService);
        */
				 
		userDao.deleteAll();			  
		for(User user : users) userDao.add(user);
		
		try {
			this.testUserService.upgradeLevels();
			fail("TestUserServiceException expected"); 
		}
		catch(TestUserServiceException e) { 
		}
		
		checkLevelUpgraded(users.get(1), false);
	}

	@Test
	public void advisorAutoProxCreator(){
		assertThat(testUserService, is(java.lang.reflect.Proxy.class));
		assertThat(userService, is(java.lang.reflect.Proxy.class));
	}

	// 포인트컷의 클래스 필터에 선정되도록 이름 변경. 포인트컷은 *ServiceImpl 클래스들로 설정해놨다.
	static class TestUserServiceImpl extends UserServiceImpl {
		private String id = "madnite1";

		protected void upgradeLevel(User user) {
			if (user.getId().equals(this.id)) throw new TestUserServiceException();
			super.upgradeLevel(user);
		}
	}
	
	static class TestUserServiceException extends RuntimeException {
	}



}

