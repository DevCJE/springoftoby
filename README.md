■ 자동 프록시 생성

투명한 부가기능을 적용하는 과정에서 발견됐던 대부분의 문제는 해결되었다. 타깃 코드는 여전히 깔끔한 채로 남아 있고, 부가기능은 한 번만 만들어 모든 타깃과 메소드에 재사용이 가능하고, 타깃의 적용 메소드를 선정하는 방식도 독립적으로 작성할 수 있도록 분리되어 있다.

하지만 아직 한 가지 해결할 과제가 남아 있다. 부가기능의 적용이 필요한 타깃 오브젝트마다 거의 비슷한 내용의 ProxyFactoryBean 빈 설정정보를 추가해주는 부분이다. 새로운 타깃이등장했다고 해서 코드를 손댈 필요는 없어졌지만, 설정은 매번 복사해서 붙이고 target 프로퍼티의 내용을 수정해줘야 한다. 이 경우 target 프로퍼티를 제외하면 빈 클래스의 종류, 어드바이스, 포인트컷의 설정이 동일하다.

■ 중복 문제의 접근 방법

지금까지 다뤄봤던 반복적이고 기계적인 코드에 대한 해결책을 생각해보자. JDBC API를 사용하는 DAO 코드의 경우 바뀌지 않는 부분과 바뀌는 부분을 구분해서 분리하고, 템플릿과 콜백, 클라이언트로 나누는 방법을 통해 깔끔하게 해결했다. 전략 패턴과 DI를 적용한 덕분이다. 그런데 이와는 좀 다른 방법으로 반복되는 코드의 문제를 해결했던 것이 있다. 바로 반복적인 위임 코드가 필요한 프록시 클래스 코드다. 타깃 오브젝트로의 위임 코드와 부가기능 적용을 위한 코드가 프록시가 구현해야 하는 모든 인터페이스 메소드마다 반복적으로 필요했다. 이는 단순한 분리와 DI와는 다른 독특한 방법으로 해결했다. 다이내믹 프록시라는 런타임 코드 자동생성 기법을 이용한 것이다. 변하지 않는 타깃으로의 위임과 부가기능 적용 여부 판단이라는 부분은 코드 생성 기법을 이용하는 다이내믹 프록시 기술에 맡기고, 변하는 부가기능 코드는 별도로 만들어서 다이내믹 프록시 생성 팩토리에 DI로 제공하는 방법을 사용한 것이다. 의미있는 부가기능 로직인 트랜잭션 경계설정은 코드로 만들게 하고, 기계적인 코드인 타깃 인터페이스 구현과 위임, 부가기능 연동 부분은 자동생성하게 한 것이다.

반복적인 프록시의 메소드 구현을 코드 자동생성 기법을 이용해 해결했다면 반복적인 ProxyFactoryBean 설정 문제는 설정 자동등록 기법으로 해결할 수 없을까? 또는 실제 빈 오브젝트가 되는 것은 ProxyFactoryBean을 통해 생성되는 프록시 그 자체이므로 프록시가 자동으로 빈으로 생성되게 할 수는 없을까? 마치 다이내믹 프록시가 인터페이스만 제공하면 모든 메소드에 대한 구현 클래스를 자동으로 만들듯이, 일정한 타깃 빈의 목록을 제공하면 자동으로 각 타깃 빈에 대한 프록시를 만들어주는 방법이 있다면 ProxyFactoryBean 타입 빈 설정을 매번 추가해서 프록시를 만들어내는 수고를 덜 수 있을 것 같다.

■ 빈 후처리기를 이용한 자동 프록시 생성기

스프링은 컨테이너로서 제공하는 기능 중에서 변하지 않는 핵심적인 부분외에는 대부분 확장할 수 있도록 확장 포인트를 제공해준다. 그 중에서 관심을 가질 만한 확장 포인트는 바로 BeanPostProcessor라는 인터페이스를 구현해서 만드는 빈 후처리기이다. 빈 후처리기는 이름 그대로 스프링 빈 오브젝트로 만들어지고 난 후에, 빈 오브젝트를 다시 가공할 수 있게 해준다. 여기서는 그 빈 후처리기중 하나인 DefaultAdvisorAutoProxyCreator를 살펴보겠다. 이름에서 알 수 있듯이 어드바이저를 이용한 자동 프록시 생성기다.

빈 후처리기를 스프링에 적용하는 방법은 빈 후처리기 자체를 빈으로 등록하는 것이다. 스프링은 빈 후처리기가 빈으로 등록되어 있으면 빈 오브젝트가 생성될 때마다 빈 후처리기에 보내서 후처리 작업을 요청한다. 빈 후처리기는 빈 오브젝트의 프로퍼티를 강제로 수정할 수도 있고 별도의 초기화 작업을 수행할 수도 있다. 심지어는 만들어진 빈 오브젝트 자체를 바꿔치기 할 수도 있다. 따라서 스프링이 설정을 참고해서 만든 오브젝트가 아닌 다른 오브젝트를 빈으로 등록시키는 것이 가능하다. 이를 잘 이용하면 스프링이 생성하는 빈 오브젝트의 일부를 프록시로 포장하고, 프록시를 빈으로 대신 등록할 수도 있다. 바로 이것이 자동 프록시 생성 빈 후처리기이다.

DefaultAdvisorAutoProxyCreator 빈 후처리기가 등록되어 있으면 스프링은 빈 오브젝트를 만들 때마다 후처리기에게 빈을 보낸다. DefaultAdvisorAutoProxyCreator는 빈으로 등록된 모든 어드바이저 내의 포인트컷을 이용해 전달받은 빈이 프록시 적용 대상인지 확인한다. 프록시 적용대상이면 그때는 내장된 프록시 생성기에게 현재 빈에 대한 프록시를 만들게 하고, 만들어진 프록시에 어드바이저를 연결해준다. 빈 후처리기는 프록시가 생성되면 원래 컨테이너가 전달해준 빈 오브젝트 대신 프록시 오브젝트를 컨테이너에게 돌려준다. 컨테이너는 최종적으로 빈 후처리기가 돌려준 오브젝트를 빈으로 등록하고 사용한다. 적용할 빈을 선정하는 로직이 추가된 포인트컷이 담긴 어드바이저를 등록하고 빈 후처리기를 사용하면 일일이 ProxyFactoryBean 빈을 등록하지 않아도 타깃 오브젝트에 자동으로 프록시가 적용되게 할 수 있다. 마지막 남은 번거로운 ProxyFactoryBean 설정 문제를 말끔하게 해결해주는 놀라운 방법이다.

■ 확장된 포인트컷

지금까지 포인트컷이란 타깃 오브젝트의 메소드 중에서 어떤 메소드에 부가기능을 적용할지를 선정해주는 역할을 하였지만 사실 포인트컷은 어떤 빈에 프록시를 적용할지 선택하는 기능도 담겨 있다.

public interface Pointcut{
    // 프록시를 적용할 클래스인지 확인해준다.
    ClassFilter getClassFilter();
    // 어드바이스를 적용할 메소드인지 확인해준다.
    MethodMatcher getMethodMatcher();
}
이전에 우리가 사용했던 NameMatchMethodPointcut은 메소드 선별 기능만 가진 특별한 포인트컷이다. 메소드만 선별한다는 건 클래스 필터는 모든 클래스를 다 받아주도록 만들어져 있다는 뜻이다. 따라서 클래스의 종류는 상관없이 메소드만 판별한다. 어차피 ProxyFactoryBean에서 포인트컷을 사용할 때는 이미 타깃이 정해져 있기 때문에 포인트 컷은 메소드 선별만 해주면 그만이었다. 만약 Pointcut 선정 기능을 모두 적용한다면 먼저 프록시를 적용할 클래스인지 판단하고 나서, 적용 대상 클래스인 경우에는 어드바이스를 적용할 메소드인지 확인하는 식으로 동작한다.

ProxyFactoryBean에서는 굳이 클래스 레벨의 필터는 필요 없었지만, 모든 빈에 대해 프록시 자동 적용 대상을 선별해야 하는 빈 후처리기인 DefaultAdivsorAutoProxyCreator는 클래스와 메소드 선정 알고리즘을 모두 갖고 있는 포인트 컷이 필요하다.

■ 포인트컷 테스트

이제 포인트컷을 확장해서 클래스를 판별하고 이후에 메소드 선정 알고리즘을 적용하게 해보자.

 @Test
    public void classNamePointcutAdvisor() {
        NameMatchMethodPointcut classMethodPointcut = new NameMatchMethodPointcut() {
            public ClassFilter getClassFilter() { // 익명 내부 클래스 방식으로 클래스를 정의한다.
                return new ClassFilter() {
                    public boolean matches(Class<?> clazz) {                        
                        return clazz.getSimpleName().startsWith("HelloT"); // 클래스 이름이 HelloT로 시작하는 것만 선정한다.
                    }
                };
            }
        };
        classMethodPointcut.setMappedName("sayH*"); // sayH로 시작하는 메소드 이름을 가진 메소드만 선정한다.

        // 테스트
        checkAdviced(new HelloTarget(), classMethodPointcut, true); // 적용 클래스

        class HelloWorld extends HelloTarget {};
        checkAdviced(new HelloWorld(), classMethodPointcut, false); // 미적용 클래스

        class HelloToby extends HelloTarget {};
        checkAdviced(new HelloToby(), classMethodPointcut, true); // 적용 클래스
    }

                                                                // 적용 대상인가?
    private void checkAdviced(Object target, Pointcut pointcut, boolean adviced) {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(target);
        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
        Hello proxiedHello = (Hello) pfBean.getObject();

        if (adviced) {
            // 메소드 선정 방식을 통해 어드바이스 적용
            assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
            assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }
        else {
            // 어드바이스 적용 대상 후보에서 아예 탈락
            assertThat(proxiedHello.sayHello("Toby"), is("Hello Toby"));
            assertThat(proxiedHello.sayHi("Toby"), is("Hi Toby"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }
    }
포인트컷은 NameMatchMethodPointcut을 내부 익명 클래스 방식으로 확장해서 만들었다. 원래 모든 클래스를 다 받아주는 클래스 필터를 리턴하던 getClassFilter()를 오버라이드해서 이름이 HelloT로 시작하는 클래스만을 선정해주는 필터로 만들었다. 메소드 이름 선정기준은 기존에 사용한 것을 그대로 유지했다.

테스트는 세 가지 클래스에 대해 진행한다. 모두 기존에 있던 HelloTarget이라는 클래스를 그대로 상속한 것이라 메소드 내용은 동일하다. 단지 클래스 이름만 다를 뿐이다. 이 세 개의 클래스에 모두 동일한 포인트컷을 적용했다. 메소드 선정기준으로만 보자면 두 개의 메소드에는 어드바이스를 적용하고 마지막 것은 적용되지 않으면 된다. 하지만 두 번째 HelloWorld라는 클래스는 클래스 필터에서 이미 탈락해버리기 때문에 메소드 이름과 무관하게 모든 메소드가 어드바이스 적용 대상에서 제외된다.

포인트컷이 클래스 필터까지 동작해서 클래스를 걸러버리면 아무리 프록시를 적용했다고 해도 부가기능은 전혀 제공되지 않는다는 점에 주의해야 한다. 사실 클래스 필터에서 통과하지 못한 대상은 프록시를 만들 필요조차 없다. 어차피 어떤 메소드에도 부가기능이 적용되지 않을 텐데 굳이 프록시를 둘 이유가 없기 때문이다.

■ DefaultAdvisorAutoProxyCreator의 적용

프록시 자동생성 방식에서 사용할 포인트컷을 만드는 방법을 학습 테스트를 만들어가면서 살펴봤으니, 이제 실제로 적용할 차례다.

만들어야 할 클래스는 하나뿐이다. 포인트컷으로 이용했던 NameMatchMethodPointcut을 상속해서 프로퍼티로 주어진 이름 패턴을 가지고 클래스 이름을 비교하는 ClassFilter를 추가하도록 만들 것이다.

public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {
    // 모든 클래스를 다 허용하던 디폴트 클래스 필터를 프로퍼티로 받은 클래스 이름을 이용해서 필터를 만들어 덮어씌운다.
    public void setMappedClassName(String mappedClassName){
        this.setClassFilter(new SimpleClassFilter(mappedClassName));
    }

    static class SimpleClassFilter implements ClassFilter{
        String mappedName;

        private SimpleClassFilter(String mappedName) {
            this.mappedName = mappedName;
        }

        @Override
        public boolean matches(Class<?> clazz) {
            // 와일드카드(*)가 들어간 문자열 비교를 지원하는 스프링의 유틸리티 메소드다.
            // *name, name*, *name* 세 가지 방식을 모두 지원한다.
            return PatternMatchUtils.simpleMatch(mappedName, clazz.getSimpleName());
        }
    }
}
이제 적용할 자동 프록시 생성기인 DefaultAdvisorAutoProxyCreator는 등록된 빈 중에서 Advisor 인터페이스를 구현한 것을 모두 찾는다. 그리고 생성되는 모든 빈에 대해 어드바이저의 포인트컷을 적용해보면서 프록시 적용 대상을 선정한다. 빈 클래스가 프록시 선정 대상이라면 프록시를 만들어 원래 빈 오브젝트와 바꿔치기한다. 원래 빈 오브젝트는 프록시 뒤에 연결돼서 프록시를 통해서만 접근 가능하게 바뀌는 것이다. 따라서 타깃 빈에 의존한다고 정의한 다른 빈들은 프록시 오브젝트를 대신 DI 받게 될 것이다.

이제 자동 프록시 생성기를 스프링 빈으로 등록하면 된다. 이때에 id 어트리뷰트는 해당 빈이 참조되거나 조회될 필요가 없기에 넣지 않아도 무방하다. 이후 포인트컷을 등록한다.

<bean id ="transactionPointcut" class="springbook.user.service.NameMatchClassMethodPointcut">
		<property name="mappedClassName" value="*ServiceImpl"/>
		<property name="mappedName" value="upgrade*"/>
</bean>
어드바이스인 transactionAdvice 빈의 설정은 수정할 게 없다. 어드바이저 또한 마찬가지이다. 하지만 어드바이저로서 사용되는 방법이 바뀌었다는 사실은 기억해두자. ProxyFactoryBean으로 등록한 빈에서 명시적으로 DI 하는 빈은 존재하지 않고 대신 어드바이저를 이용하는 자동 프록시 생성기인 DefaultAdvisorAutoProxyCreator에 의해 자동수집되고, 프록시 대상 선정 과정에 참여하며, 자동생성된 프록시에 다이내믹하게 DI 돼서 동작하는 어드바이저가 된다.

자동 프록시 생성기를 사용함으로써 기존 테스트에 문제가 발생하는데 타깃을 테스트용 클래스로 바꿔치기 하는 방법을 사용했지만 이제는 자동 생성기에서 프록시만을 남겨줬을뿐 ProxyFactoryBean 같은게 남아 있지 않기에 더 이상 이 방법은 불가하다. 이제 자동 프록시 생성기라는 스프링 컨테이너에 종속적인 기법을 사용했기 때문에 예외상황을 위한 테스트 대상도 빈으로 등록해 줄 필요가 있다. 이제는 타깃을 코드에서 바꿔치기할 방법도 없을뿐더러, 자동 프록시 생성기의 적용이 되는지도 빈을 통해 확인할 필요가 있기 때문이다.

// 포인트컷의 클래스 필터에 선정되도록 이름 변경. 포인트컷은 *ServiceImpl 클래스들로 설정해놨다.
	static class TestUserServiceImpl extends UserServiceImpl {
		private String id = "madnite1"; // 테스트 픽스처의 값을 이제 가져올 수 없기에 고정

		protected void upgradeLevel(User user) {
			if (user.getId().equals(this.id)) throw new TestUserServiceException();
			super.upgradeLevel(user);
		}
	}
이제 TestUserServiceImpl을 빈으로 등록하자.

<bean id="testUserService" 
      class="springbook.user.service.UserServiceTest$TestUserServiceImpl" 
      parent="userService" />
여기서 특이한 사항이 두 가지 눈에 띄는데, 하나는 클래스 이름에 사용한 $ 기호인데, 이는 스태틱 멤버 클래스를 지정할 때 사용하는 것이다. TestUserServiceImpl 클래스는 UserServiceTest의 스태틱 멤버 클래스이므로 $를 사용해서 클래스 이름을 지정해주면 된다.

또 한가지 특이한 점은 parent 애트리뷰트다. 태그에 parent 애트리뷰트를 사용하면 다른 빈 설정의 내용을 상속 받을 수 있다. 현재 parent='userService"라고 하면 userService의 빈 설정을 그대로 가져와서 사용하겠다는 뜻이다.

그리고 이제 이렇게 추가한 testUserService 빈을 이용해서 테스트를 진행하면 테스트가 성공하는 것을 확인할 수 있다.

public class UserServiceTest {

	@Autowired UserService userService;	
	// 같은 타입의 빈이 두 개 존재하기 때문에 필드의 이름을 기준으로 주입될 빈이 결정된다.
	// 자동 프록시 생성기에 의해 트랜잭션 부가기능이 제대로 적용되었는지 확인하는 것이 목적이다.
	@Autowired UserService testUserService;
    ....

    @Test
	public void upgradeAllOrNothing() throws Exception {
				 
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
}
■ 포인트컷 표현식을 이용한 포인트컷

이번에는 좀 더 편리한 포인트컷 작성 방법을 알아보자. 지금까지 사용했던 포인트컷은 메소드의 이름과 클래스의 이름 패턴을 각각 클래스 필터와 메소드 매처 오브젝트로 비교해서 선정하는 방식이었다. 일일이 클래스 필터와 메소드 매처를 구현하거나, 스프링이 제공하는 필터나 매처 클래스를 가져와 프로퍼티를 설정하는 방식을 사용해야 했다.

지금까지는 단순한 이름을 비교하는게 전부였지만 더 복잡하고 세밀한 기준을 이용해 클래스나 메소드를 선정하게 하려면 어떻게 해야 할까? 스프링은 아주 간단하고 효과적인 방법으로 포인트컷의 클래스와 메소드를 선정하는 알고리즘을 작성할 수 있는 방법을 제공한다. 표현식 언어를 사용해서 포인트 컷을 작성할 수 있도록 하는 것인데 이것을 포인트컷 표현식이라고 부른다.

■ 포인트컷 표현식 포인트컷 표현식을 지원하는 포인트컷을 적용하려면 AspectJExpressionPointcut 클래스를 사용하면 된다. 앞에서 만들었던 NameMatchClassMethodPointcut은 클래스와 메소드의 이름의 패턴을 독립적으로 비교하도록 만들어져 있다. 이를 위해 비교할 조건을 가진 두 가지 패턴을 프로퍼티로 넣어줬다. 하지만 AspetJExpressionPointcut은 클래스와 메소드의 선정 알고리즘을 포인트컷 표현식을 이용해 한 번에 지정할 수 있게 해준다. 포인트컷 표현식은 자바의 RegEx클래스가 지원하는 정규식처럼 간단한 문자열로 복잡한 선정조건을 쉽게 만들어낼 수 있다.

포인트컷 지시자 중에서 가장 대표적으로 사용되는 것은 execution()이다.

execution([접근제한자 패턴] 리턴 값 패턴 [패키지와 클래스 패턴.]
           메소드 패턴(패러미터 패턴 | "..", ...) [throws 예외 패턴])
막상 보면 어려울 수 있지만 메소드의 풀 시그니처를 문자열로 비교하는 개념이라고 보면 된다. 쉬운 이해를 위해 리플렉션으로 Target 클래스의 minus()라는 메소드의 풀시그니처를 가져와 비교해보도록 하자.

public int 
springbook.learningtest.spring.pointcut.Target.minus(int,int) 
throws java.lang.RuntimeException
처음 public은 접근제한자다. 포인트컷 표현식에서는 생략할 수 있다. 생략이 가능하다는건 이 항목에 대해서는 조건을 부여하지 않는다는 의미다. 그 다음 나오는 int는 리턴값의 타입을 나타내는 패턴이다. 포인트컷의 표현식에서 리턴 값의 타입 패턴은 필수항목이다. 반드시 하나의 타입을 지정하던가 *를 써서 모든 타입을 다 선택하겠다고 해야 한다. 이후 나오는 spring.learningtest.spring.pointcut.Target 까지가 패키지와 타입 이름을 포함한 클래스의 타입 패턴이다. 생략 가능하며 생략한다면 모든 타입을 다 허용하겠다는 뜻이다. 뒤에 이어나오는 메소드 이름 패턴과 ' . ' 으로 연결되기 때문에 작성할 때 잘 구분해야 한다. 이후 minus()는 메소드 이름 패턴이다. 필수항목이기 때문에 반드시 적어야 한다. 클래스 타입 내의 모든 메소드에 적용하기 원한다면 *를 넣으면 된다. (int,int)는 메소드 패러미터의 타입 패턴이다. 메소드 파라미터 타입을 ' , '로 구분하면서 순서대로 적으면 된다. 파라미터가 없는 메소드를 지정하고 싶다면 ()로 적는다. 파라미터의 타입과 개수에 상관없이 모두 허용하는 패턴으로 만들려면 ",,"을 넣으면 된다 ',,,'을 이용해서 뒷부분의 파라미터 조건만 생략할 수 있다. 필수항목이므로 반드시 넣어야 한다. 마지막 throws java.lang.RuntimeException은 예외 이름에 대한 패턴이며 생략 가능하다.

Target 클래스의 minus() 메소드만 선정해주는 포인트컷 표현식을 만들고 이를 검증해보는 테스트를 작성해보자.

@Test
    public void methodSignaturePointcut() throws SecurityException, NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public int springbook.learningtest.spring.pointcut.Target.*(int,int) " +
                "throws java.lang.RuntimeException)"); // Target 클래스 minus() 메소드 시그니처

        // Target.minus() 메소드 시그니처가 맞는지 확인하기 위해서는 클래스 필터와 메소드 매처를 각각 비교해 보면 된다.
        // 해당 Target 클래스의 minus() 메소드는 시그니처의 부합하기 때문에 true이다.
        assertThat(pointcut.getClassFilter().matches(Target.class) && pointcut.getMethodMatcher().matches(
                      Target.class.getMethod("minus", int.class, int.class), null), is(true));

        // Target.plus() 메소드 시그니처가 맞는지 확인하기 위해서는 클래스 필터와 메소드 매처를 각각 비교해 보면 된다.
        // 해당 Target 클래스의 plus() 메소드는 minus()메소드가 아니므로 false 이다.
        assertThat(pointcut.getClassFilter().matches(Target.class) && pointcut.getMethodMatcher().matches(
                     Target.class.getMethod("plus", int.class, int.class),null), is(false));

        // Bean.method() 메소드 시그니처가 맞는지 확인하기 위해서는 클래스 필터와 메소드 매처를 각각 비교해 보면 된다.
        // Target 클래스가 아닌 Bean 클래스이므로 false 이다.
        assertThat(pointcut.getClassFilter().matches(Bean.class) && pointcut.getMethodMatcher().matches(
                    Target.class.getMethod("method"), null), is(false));

    }
자세한 부분은 주석으로 대신하지만 기본적으로 포인트 컷 표현식은 execution() 안에 넣은 메소드 시그니처를 통해 작성한다. 이후 이 포인트컷 표현식이 제대로 적용되었는지 확인하기 위하여서 포인트컷의 클래스 필터와 메소드 매처를 통해 직접 비교해보았다.

다양한 활용 방법을 보기 위해 테스트를 보충해보자. 앞에서 만든 Target, Bean 클래스의 6개 메소드에 대해 각각 포인트컷을 적용해서 결과를 확인하는 테스트다. 주어진 포인트컷을 이용해 특정 메소드에 대한 포인트컷을 적용해보고 결과를 확인하는 메소드를 추가한다.

// 위의 methodSignaturePointcut()에 있는 메소드 표현식을 통한 포인트컷 생성과 검증 과정을 템플릿화 하였다.
    public void pointcutMatches(String expression, Boolean expected, Class<?> clazz, String methodName, Class<?>... args)
        throws Exception {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(expression);

        assertThat(pointcut.getClassFilter().matches(clazz) &&
                              pointcut.getMethodMatcher().matches(clazz.getMethod(methodName, args), null)
                                  , is(expected));
    }
다음은 pointcutMatches() 메소드를 활용해서 타깃으로 만든 두 클래스의 모든 메소드에 대해 포인트컷 선정 여부를 확인하는 메소드를 추가한다. 표현식과 6개의 메소드에 대한 예상 결과를 주면 이를 검증하는 기능을 제공한다.

public void targetClassPointcutMatches(String expression, boolean... expected) throws Exception{
        pointcutMatches(expression, expected[0], Target.class, "hello");
        pointcutMatches(expression, expected[1], Target.class, "hello", String.class);
        pointcutMatches(expression, expected[2], Target.class, "plus", int.class, int.class);
        pointcutMatches(expression, expected[3], Target.class, "minus", int.class, int.class);
        pointcutMatches(expression, expected[4], Target.class, "method");
        pointcutMatches(expression, expected[5], Bean.class, "method");
    }
이후 이를 이용해서 테스트를 진행할 수 있다.

    @Test
    // 아래 targetClassPointcutMatches를 이용해 포인트컷 표현식에 대한 다양한 결과를 알아보는 테스트 메소드
    public void pointcut() throws Exception{
        targetClassPointcutMatches("execution(* *(..))", true, true, true, true, true, true);
        targetClassPointcutMatches("execution(* hello(..))", true, true, false, false, false, false);
        targetClassPointcutMatches("execution(* hello())", true, false, false, false, false, false);
        targetClassPointcutMatches("execution(* hello(String))", false, true, false, false, false, false);
        targetClassPointcutMatches("execution(* meth*(..))", false, false, false, false, true, true);
        targetClassPointcutMatches("execution(* *(int,int))", false, false, true, true, false, false);
        targetClassPointcutMatches("execution(* *())", true, false, false, false, true, true);
        targetClassPointcutMatches("execution(* springbook.learningtest.spring.pointcut.*.*(..))", true, true, true, true, true, true);
       // 이하 생략 
   }
위와 같이 AspectJ 포인트컷 표현식은 메소드를 선정하는 데 편리하게 쓸 수 있는 강력한 표현식 언어다. 이제 해당 표현식을 통해 transctionPointcut의 빈 설정을 변경해보자.

<bean id ="transactionPointcut" class="org.springframework.aop.aspectj.AspectJExpressionPointcut">
	<property name="expression" value="execution(* *..*ServiceImpl.upgrade*(..))"/>
</bean>
포인트컷 표현식을 사용하면 로직이 짧은 문자열에 담기기 때문에 클래스나 코드를 추가할 필요가 없어서 코드와 설정이 모두 단순해진다. 반면에 문자열로 된 표현식이므로 런타임 시점까지 문법의 검증이나 기능 확인이 되지 않는다는 단점도 있다.

다만 포인트컷 표현식에서 알아둘점은 단순하게 클래스 이름으로 비교하는 것이 아니라 타입으로 비교를 한다는 것이다. 우리는 UserServiceImpl과 TestUserServiceImpl 클래스로 등록된 두 개의 빈이 선정되어 있는데 TestUserServiceImpl의 클래스 이름을 바꿀지라도 포인트컷 표현식에서는 테스트가 성공한다. 그 이유는 TestUserServiceImpl의 슈퍼클래스는 UserServiceImpl이기 때문에 클래스명을 바꿔도 해당 타입은 UserServiceImpl이라 볼 수 있기 때문이다. ■ AOP란 무엇인가? 비즈니스 로직을 담은 UserService에 트랜잭션을 적용해온 과정을 정리해보자.

■ 트랜잭션 서비스 추상화 트랜잭션 경계설정 코드를 비즈니스 로직을 담은 코드에 넣으면서 맞닥뜨린 첫 번째 문제는 특정 트랜잭션 기술에 종속되는 코드가 되어버린다는 것이었다. JDBC의 로컬 트랜잭션 방식을 적용한 코드를, JTA를 이용한 글로벌/분산 트랜잭션 방식으로 바꾸려면 모든 트랜잭션 적용 코드를 수정해야 한다는 심각한 문제점이 발견됐다. 트랜잭션을 처리한다는 기본적인 목적은 변하지 않더라도 그것을 어떻게 해야 한다는 구체적인 방법이 변하면 트랜잭션과는 직접 관련이 없는 코드가 담긴 많은 클래스를 일일이 수정해야 했다.

그래서 트랜잭션 적용이라는 추상적인 작업 내용은 유지한 채로 구체적인 구현 방법을 자유롭게 바꿀 수 있도록 서비스 추상화 기법을 적용했다. 이 덕분에 비즈니스 로직코드는 트랜잭션을 어떻게 처리해야 한다는 구체적인 방법과 서버환경에서 종속되지 않는다. 구체적인 구현 내용을 담은 의존 오브젝트는 런타임 시에 다이내믹하게 연결해준다는 DI를 활용한 전형적인 접근 방법이었다. 트랜잭션 추상화란 결국 인터페이스와 DI를 통해 무엇을 하는지는 남기고, 그것을 어떻게 하는지를 분리한 것이다.

■ 프록시와 데코레이터 패턴 트랜잭션을 어떻게 다룰 것인가는 추상화를 통해 코드에서 제거했지만, 여전히 비즈니스 로직 코드에는 트랜잭션을 적용하고 있다는 사실은 드러나 있다. 트랜잭션이라는 부가적인 기능을 어디에 적용할 것인가는 여전히 코드에 노출시켜야 했다. 문제는 트랜잭션은 거의 대부분의 비즈니스 로직을 담은 메소드에 필요하다는 점이다. 게다가 트랜잭션의 경계설정을 담당하는 코드의 특성 때문에 단순한 추상화와 메소드 추출 방법으로는 더 이상 제거할 방법이 없었다. 그래서 도입한 것이 바로 DI를 이용해 데코레이터 패턴을 적용하는 방법이었다.

클라이언트가 인터페이스와 DI를 통해 접근하도록 설계하고, 데코레이터 패턴을 적용해서, 비즈니스 로직을 담은 클래스의 코드에는 전혀 영향을 주지 않으면서 트랜잭션이라는 부가기능을 자유롭게 부여할 수 있는 구조를 만들었다. 트랜잭션을 처리하는 코드는 일종의 데코레이터에 담겨서, 클라이언트와 비즈니스 로직을 담은 타깃 클래스 사이에 존재하도록 만들었다. 그래서 클라이언트가 일종의 대리자인 프록시 역할을 하는 트랜잭션 데코레이터를 거쳐서 타깃에 접근할 수 있게 했다. 결국 비즈니스 로직 코드는 트랜잭션과 같은 성격이 다른 코드로부터 자유로워졌고, 독립적으로 로직을 검증하는 고립된 단위 테스트를 만들 수도 있게 됐다.

■ 다이내믹 프록시와 프록시 팩토리 빈 프록시를 이용해서 비즈니스 로직 코드에서 트랜잭션 코드는 모두 제거할 수 있었지만, 비즈니스 로직 인터페이스의 모든 메소드마다 트랜잭션 기능을 부여하는 코드를 넣어 프록시 클래스를 만드는 작업이 오히려 큰 짐이 됐다. 트랜잭션 기능을 부여하지 않아도 되는 메소드조차 프록시로서 위임 기능이 필요하기 때문에 일일이 다 구현을 해줘야 했다.

그래서 프록시 클래스 없이도 프록시 오브젝트를 런타임 시에 만들어주는 JDK 다이내믹 프록시 기술을 적용했다. 그 덕분에 프록시 클래스 코드 작성의 부담도 덜고, 부가기능 부여 코드가 여기저기 중복돼서 나타나는 문제도 일부 해결할 수 있었다. 일부 메소드에만 트랜잭션을 적용해야 하는 경우에는 메소드를 선정하는 패턴 등을 이용할 수도 있었다. 하지만 동일한 기능의 프록시를 여러 오브젝트에 적용할 경우 오브젝트 단위로는 중복이 일어나는 문제는 해결하지 못했다.

JDK 다이내믹 프록시와 같은 프록시 기술을 추상화한 스프링의 프록시 팩토리 빈을 이용해서 다이내믹 프록시 생성 방법에 DI를 도입했다. 내부적으로 템플릿/콜백 패턴을 활용하는 스프링 프록시 팩토리 빈 덕분에 부가기능을 담은 어드바이스와 부가기능 선정 알고리즘을 담은 포인트컷은 프록시에서 분리될 수 있었고 여러 프록시에서 공유해서 사용할 수 있게 됐다.

■ 자동 프록시 생성 방법과 포인트컷 트랜잭션 적용 대상이 되는 빈마다 일일이 프록시 팩토리 빈을 설정해줘야 한다는 부담이 남아있었다. 이를 해결하기 위해서 스프링 컨테이너의 빈 생성 후처리 기법을 활용해 컨테이너 초기화 시점에서 자동으로 프록시를 만들어주는 방법을 도입했다. 프록시를 적용할 대상을 일일이 지정하지 않고 패턴을 이용해 자동으로 선정할 수 있도록, 클래스를 선정하는 기능을 담은 확장된 포인트컷을 사용했다. 결국 트랜잭션 부가기능을 어디에 적용하는지에 대한 정보를 포인트컷이라는 독립적인 정보로 완전히 분리할 수 있었다. 처음에는 클래스와 메소드 선정 로직을 담은 코드를 직접 만들어서 포인트컷으로 사용했지만, 최종적으로는 포인트 컷 표현식이라는 좀 더 편리하고 깔끔한 방법을 활용해서 간단한 설정만으로 적용대상을 손쉽게 선택할 수 있게 됐다.

■ 부가기능의 모듈화 관심사가 같은 코드를 분리해 한데 모으는 것은 소프트웨어 개발의 가장 기본이 되는 원칙이다. 그렇게 관심사가 같은 코드를 객체지향 설계 원칙에 따라 분리하고, 서로 낮은 결합도를 가진 채로 독립적이고 유연하게 확장할 수 있는 모듈로 만드는 것이 초난감 DAO로부터 시작해서 지금까지 해온 작업이다. 트랜잭션 코드는 부가적으로 부여되는 기능이고 애플리케이션 전반에 여기저기 흩어져 있다. 따라서 이와 같은 부가기능은 핵심기능과 같은 방식으로는 모듈화하기가 매우 힘들다. 이름 그대로 부가기능이기 때문에 스스로 독립적인 방식으로 존재해서는 적용되기 어렵기 때문이다. 트랜잭션 부가기능이란 트랜잭션 기능을 추가해줄 다른 대상, 즉 타깃이 존재해야만 의미가 있다. 따라서 각 기능을 부가할 대상인 각 타깃의 코드 안에 침투하거나 긴밀하게 연결되어 있지 않으면 안 된다. 기능이 부여되는 타깃은 애플리케이션의 핵심기능이다. 이런 핵심기능은 독립적으로 존재할 수 있으며, 테스트도 가능하고, 최소한의 인터페이스를 통해 다른 모듈과 결합해 사용되면 된다. 반면에 부가기능은 여타 핵심기능과 같은 레벨에서는 독립적으로 존재하는 것 자체가 불가능하다.

그래서 많은 개발자들은 이런 부가기능을 어떻게 독립적인 모듈로 할 것인지 고민하였고 지금까지 살펴본 DI, 데코레이터 패턴, 다이내믹 프록시, 오브젝트 생성 후처리, 자동 프록시 생성, 포인트컷과 같은 기법은 이런 문제를 해결하기 위해 적용한 대표적인 방법이다. 덕분에 부가기능인 트랜잭션 경계설정 기능은 TransactionAdvice라는 이름으로 모듈화될 수 있었다. 독립적으로 모듈화되어 있기 때문에 이 코드는 중복되지 않으며, 변경이 필요하면 한 곳만 수정하면 된다. 또한 포인트컷이라는 방법을 통해 부가기능을 부여할 대상을 선정할 수 있었다.

결국 지금까지 해온 모든 작업은 핵심기능에 부여되는 부가기능을 효과적으로 모듈화하는 방법을 찾는 것이었고, 어드바이스와 포인트컷을 결합한 어드바이저가 단순하지만 이런 특성을 가진 모듈의 원시적인 형태로 만들어지게 됐다.

■ AOP : 애스펙트 지향 프로그래밍 전통적인 객체지향 기술의 설계 방법으로는 독립적인 모듈화가 불가능한 트랜잭션 경계설정과 같은 부가기능을 어떻게 모듈화할 것인가를 연구해온 사람들은, 이 부가기능 모듈화 작업은 기존의 객체지향 설계 패러다임과는 구분되는 새로운 특성이 있다고 생각했다. 그래서 이런 부가기능 모듈을 객체지향 기술에서 주로 사용하는 오브젝트와는 다르게 애스펙트(aspect)라고 부르기 시작했다.

애스펙트란 그 자체로 어플리케이션의 핵심기능을 담고 있지는 않지만, 애플리케이션을 구성하는 중요한 한 가지 요소이고, 핵심기능에 부가되어 의미를 갖는 특별한 모듈을 가르킨다. 애스펙트는 부가될 기능을 정의한 코드인 어드바이스와, 어드바이스를 어디에 적용할지를 결정하는 포인트컷을 함께 갖고 있다. 지금 사용하고 있는 어드바이저는 아주 단순한 형태의 애스펙트라고 볼 수 있다.

이렇게 애플리케이션의 핵심적인 기능에서 부가적인 기능을 분리해서 애스펙트라는 독특한 모듈로 만들어서 설계하고 개발하는 방법을 애스펙트 지향 프로그래밍(Aspect Oriented Programming)또는 약자로 AOP라고 부른다. 이름만 들으면 마치 OOP가 아닌 다른 프로그래밍 언어 또는 패러다임이라고 느껴지지만, AOP는 OOP를 돕는 보조적인 기술이지 OOP를 완전히 대체하는 새로운 개념은 아니다. AOP는 애스펙트를 분리함으로써 핵심기능을 설계하고 구현할 때 객체지향적인 가치를 지킬 수 있도록 도와주는 것이라고 보면 된다. 트랜잭션 기술의 적용에만 주목하고 싶다면 TransactionAdvice에만 집중하면 된다. 그리고 그 대상을 결정해주는 transactionPointcut 빈의 설정만 신경 써주면 된다. 이렇게 애플리케이션을 특정한 관점을 기준으로 바라볼 수 있게 해준다는 의미에서 AOP를 관점 지향 프로그래밍이라고도 한다.

■ AOP 적용기술

■ 프록시를 이용한 AOP 스프링은 IoC/DI 컨테이너와 다이내믹 프록시, 데코레이터 패턴, 프록시 패턴, 자동 프록시 생성 기법, 빈 오브젝트의 후처리 조작 기법 등의 다양한 기술을 조합해 AOP를 지원하고 있다. 그중 가장 핵심은 프록시를 이용했다는 것이다. 프록시로 만들어서 DI로 연결된 빈 사이에 적용해 타깃의 메소드 호출 과정에 참여해서 부가기능을 제공해주도록 만들었다. 따라서 스프링 AOP는 자바의 기본 JDK와 스프링 컨테이너 외에는 특별한 기술이나 환경을 요구하지 않는다.

스프링 AOP의 부가기능을 담은 어드바이스가 적용되는 대상은 오브젝트의 메소드다. 프록시 방식을 사용했기 때문에 메소드 호출 과정에 참여해서 부가기능을 제공해주게 되어 있다. 어드바이스가 구현하는 MethodInterceptor 인터페이스는 다이내믹 프록시의 InvocationHandler와 마찬가지로 프록시로부터 메소드 요청정보를 전달받아서 타깃 오브젝트의 메소드를 호출한다. 타깃의 메소드를 호출하는 전후에 다양한 부가기능을 제공할 수 있다. 독립적으로 개발한 부가기능 모듈을 다양한 타깃 오브젝트의 메소드에 다이내믹하게 적용해주기 위해 가장 중요한 역할을 맡고 있는 게 바로 프록시다. 그래서 스프링 AOP는 프록시 방식의 AOP라고 할 수 있다,

■ 바이트코드 생성과 조작을 통한 AOP 가장 강력한 AOP 프레임워크로 꼽히는 AspectJ는 프록시를 사용하지 않는 대표적인 AOP 기술이다. 스프링도 AspectJ의 뛰어난 포인트컷 표현식을 차용해서 사용할 만큼 매우 성숙하고 발전한 AOP 기술이다. AspectJ는 스프링처럼 다이내믹 프록시 방식을 사용하지 않는다. AspectJ는 프록시처럼 간접적인 방법이 아니라, 타깃 오브젝트를 뜯어고쳐서 부가기능을 직접 넣어주는 직접적인 방법을 사용한다. 부가기능을 넣는다고 타깃 오브젝트의 소스코드를 수정할 수는 없으니, 컴파일된 타깃의 클래스 파일 자체를 수정하거나 클래스가 JVM에 로딩되는 시점을 가로채서 바이트코드를 조작하는 복잡한 방법을 사용한다. 트랜잭션 코드가 UserService 클래스에 비즈니스 로직과 함께 있었을 때처럼 만들어버리는 것이다.

AspectJ가 이렇게 복잡한 방법을 사용하는 이유는 두 가지로 생각해 볼 수 있다. 첫째는 바이트코드를 조작해서 타깃 오브젝트를 직접 수정해버리면 스프링과 같은 DI 컨테이너의 도움을 받아서 자동 프록시 생성 방식을 사용하지 않아도 AOP를 적용할 수 있기 때문이다. 둘째는 프록시 방식보다 훨씬 강력하고 유연한 AOP가 가능하기 때문이다. 프록시를 AOP의 핵심 메커니즘으로 사용하면 부가기능을 부여할 대상은 클라이언트가 호출할 때 사용하는 메소드로 제한한다. 하지만 바이트코드를 직접 조작해서 AOP를 적용하면 오브젝트의 생성, 필드 값의 조회와 조작, 스태틱 초기화 등의 다양한 작업에 부가기능을 부여해줄 수 있다.

■ AOP 네임스페이스 스프링 AOP를 적용하기 위해 추가했던 어드바이저, 포인트컷, 자동 프록시 생성기 같은 빈들은 애플리케이션의 로직을 담은 UserDao나 UserService 빈과는 성격이 다르다. 비즈니스 로직이나 DAO처럼 애플리케이션의 일부 기능을 담고 있는 것도 아니고, dataSource 빈처럼 DI를 통해 애플리케이션 빈에서 사용되는 것도 아니다. 이런 빈들은 스프링 컨테이너에 의해 자동으로 인식돼서 특별한 작업을 위해 사용된다. 스프링의 프록시 방식 AOP를 적용하려면 최소한 네 가지 빈을 등록해야 한다.

자동 프록시 생성기, 어드바이스, 포인트컷, 어드바이저인데 부가기능을 담은 코드로 만든 어드바이저를 빼고 나머지 세 가지는 모두 스프링이 직접 제공하는 클래스를 빈으로 등록하고 프로퍼티 설정만 해준 것이다. 스프링에서는 이렇게 AOP를 위해 기계적으로 적용하는 빈들을 간편한 방법으로 등록할 수 있다. 스프링은 AOP와 관련된 태그를 정의해둔 aop 스키마를 제공한다.

<aop:config>
   <aop:pointcut id="transactionPointcut"  expression="execution(* *..*ServiceImpl.upgrade*(..))" />    
   <aop:advisor pointcut-ref="transactionPointcut"  advice-ref="transactionAdvice"  />
</aop:config>
포인트컷이나 어드바이저, 자동 포인트컷 생성기 같은 특별한 기능을 가진 빈들은 별도의 스키마에 정의된 전용 태그를 사용해 정의해주면 편리하다. 애플리케이션을 구성하는 컴포넌트 빈과 컨테이너에 의해 사용되는 기반 기능을 지원하는 빈은 구분이 되는 것이 좋다. 태그를 사용했을 때와 비교해보면 이해하기도 쉬울뿐더러 코드의 양도 대폭 줄었다.




매핑의 URL에 { } 로 들어가는 패스 변수(path variable)를 받는다.

요청 파라미터를 URL의 쿼리 스트링으로 보내는 대신 URL 패스로 풀어서 쓰는 방식을 쓰는 경우 매우 유용하다.

원래라면 id가 10인 사용자를 조회하는 페이지의 URL을 쿼리 스트링으로 파라미터를 전달하면 보통 다음과 같다

/user/view?id=10
파라미터를 URL 경로에 포함시키는 방식으로 하면 이해하기 쉽고 보기 좋은 URL을 다음과 같이 만들 수 있다.

/user/view/10
문제는 이렇게 일부가 달라질 수 있는 URL을 특정 컨트롤러에 매핑하는 방법과 URL 중에서 파라미터에 해당하는 값을 컨트롤러에서 참조하는 방법이다.

@Controller는 URL에서 파라미터에 해당하는 부분을 { }을 넣는 URI 템플릿을 사용할 수 있다. 컨트롤러 메소드 파라미터에는 @PathVariable 애노테이션을 이용해 URI 템플릿 중에서 어떤 파라미터를 가져올지를 지정할 수 있다.

@RequestMapping("/user/view/{id}")
public String view(@Pathvariable("id")int id){
   ...
}
URL의 { }에는 패스 변수를 넣는다. 이 이름을 @PathVariable 애노테이션의 값으로 넣어서 메소드 파라미터에 부여해주면 된다. /user/view/10이라는 URL이라면, id 파라미터에 10이라는 값이 들어올 것이다. 파라미터의 타입은 URL의 내용이 적절히 변환될 수 있는 것을 사용해야 한다. 위와 같이 int 타입을 썻을 경우에는 반드시 해당 패스 변수 자리에 숫자 값이 들어 있어야 한다.  그렇지 않을 경우 HTTP 400 - Bad Request 가 발생한다.

패스 변수는 "/member/{membercode}/order/{orderid}"처럼 여러 개를 선언할 수도 있다.



현재 공부하고 있는 부분에서는 HTML 템플릿으로 mustache를 사용하고 있는데 이를 AWS에서 jar 파일로 배포할 경우 제대로 적용되지 않고 있는 경우가 생겼다.

이럴 경우 jar 파일을 war 파일로 배포해야하며, 스프링부트에서 쓰는 내부톰캣을 외부톰캣으로 변경해줘야하는데 이 부분에 대해서 알아보자.

먼저 pom.xml <packaging> 부분이 jar로 되어있는 것을 war로 바꾸어준다.

이후 외부 톰캣 사용을 위해서 의존성 추가를 해준다. 

<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-tomcat</artifactId>
	<scope>provided</scope>
</dependency>
다만 위와 같이 추가 할 경우 인텔리제이에 버그로 인해 로컬호스트로 진입하지 못하는 문제가 발생하므로 scope 부분을 제거해주던가. 주석처리한 후 작업해주어야 한다.

이후 해당 AWS 서버에 외부 톰캣을 설치해주고
해당 톰캣 webapps/ROOT 폴더에 war 파일을 배포함으로써
외부 톰캣을 실행한다.

@ModelAttribute



HTTP Request에 포함된 파라미터를 지정한 클래스의 객체로 바인딩함. 



@ModelAttribute의 'name'으로 정의한 Model객체를 다음 View에서 사용 가능



또한 name을 정의하지 않으면 어노테이션을 준 객체의 클래스 이름명으로 정의된다.



아래와 같이 패러미터 안에서 정의하는 것이 아니라 메소드에 바로 어노테이션을 정의하면 해당 컨트롤러가 시작시



해당 name을 가진 모델로 객체가 자동 생성된다.

@ModelAttribute("user") // 메소드에 ModelAttribute("name")을 통해 attribute를 정의하였다.
public ModelUser userTest(){
   ModelUser user = new ModelUser();
   user.setUser_id("1234");
   user.setUser_carnum("1004");
   user.setUser_phone("1234-1234");
   return user;
}



또한 @ModelAttribute로 설정한 모델이 이미 존재한다면 해당 모델을 불러들인 이후에 패러미터의 값을 바인딩하기 시작한다.



@RequestMapping(value = "/register", method = RequestMethod.POST)
public String register(Model model
        , @ModelAttribute("user") ModelUser user) {
    logger.info("register : POST");

    if(user.getUser_carnum() == null){
    user.setUser_lv(1);
    }
    else{
    user.setUser_lv(2);
    }
    usersvr.insertUser(user);

    return "home";
}



따라서 위를 응용하여서 유저의 정보를 수정할 때에 사용하여도 되겠으나 해당 부분을 더욱 간편하게 해주는 것이

@SessionAttributes 이다.

쓰게 된 이유?



그 동안 본인은 국비지원 학원을 수강하면서 알고 있는 DB라면 Oracle, MySQL이 전부였다.



이후 학원을 수료하고 노트북을 포맷하게 되었는데 그 덕분에 학원에서 배우기 위해 사용했던 

Oracle Database 또한 사라졌다. Oracle DB의 경우 백그라운드에서 상주하는 메모리의 양이 꽤나 묵직하고, 

삭제해도 제대로 삭제되지 않는다는 소리를 들어서..

깔끔하게 밀어버린 상태의 노트북을 다시 더럽히는(?) 것 같아서 다른 대안을 찾다가 나온 방법은 2가지였다.



한 가지는 RDBMS를 사용해야 하는 경우 (기존 프로젝트를 불러온다던가, 

JDBC 드라이버를 사용하며 RDMBS에 맞춰진 책을 배운다던가)에는 학원에서 사용했던 

서버를 쓰는 방법 (현재는 학원 서버가 복구 불가능 상태가 되어버려.. MariaDB로 교체 하였다.)과 

함께 나머지 하나는 MongoDB를 사용하는 것이었다.



왜 MongoDB를 사용해야 하냐?고 물어본다면 그 당시에는 그냥 "써보고 싶어서" 였다. 

검색을 하면서 알게 된 몇가지 이유 때문이기도 한데



1. 기존 RDBMS가 아닌 NoSQL 스토리지 이다.

2. 스키마를 설정하지 않아도 된다.

3. BSON(Binary JSON)형태로 데이터가 저장되어 확장성에 용이하다.

4. 추후 개인적으로 배우고 싶은 ORM(Object-relational mapping)을 사용할 때 더 유용해 보인다.



와 같은 이유였다. 현재는 MongoDB는 Node.js 와 함께 mongoose 라이브러리를 이용하여 연습하고 있다.

편한 부분도 있지만 기존 RDBMS에 존재하던 부분도 사라진 것이 있어 장단점이 있다고 할 수 있다.







서론이 길었고 이왕 포스트의 제목이 MongDB란? 이란 제목을 달았으므로 조금 더 자세히 알아보도록 하겠다.







MongoDB가 뭔가요?
MongoDB는 10gen (최근 MongoDB로 사명이 변경되었습니다.) 에서 만든 document 기반의 NoSQL 스토리지입니다. 

엔진은 C++로 작성되었으며, 오픈 소스입니다. 대표적인 특징으로는 다음과 같은 것들이 있습니다.

- Document-Oriented Storage : 모든 데이터가 JSON 형태로 저장되며 schema가 없습니다.
- Full Index Support : RDBMS에 뒤지지 않는 다양한 인덱싱을 제공합니다.
- Replication & High Availability : 데이터 복제를 통해 가용성을 향상시킬 수 있습니다.
- Auto-Sharding : Primary key를 기반으로 여러 서버에 데이터를 나누는 scale-out이 가능합니다.
- Querying : key 기반의 get, put 뿐만이 아니라 다양한 종류의 쿼리들을 제공합니다.
- Fast In-Place Updates : 고성능의 atomic operation을 지원합니다.
- Map/Reduce : 맵리듀스를 지원합니다.
- GridFS : 별도 스토리지 엔진을 통해 파일을 저장할 수 있습니다.

출처 : Bigmatch - 이음소시어스 개발팀 블로그



지금 당장에는 세부적인 MongoDB의 특징은 모르니 차치하고 크게 몇가지만 나열하면

문서지향 데이터베이스로 BSON 형태로 각 문서가 저장되며 Array(배열)이나 Date(날짜) 등 

기존 RDBMS에서 지원하지 않던 형태로도 저장할 수 있기 때문에 JOIN이 필요없이 

한 문서에 좀 더 이해하기 쉬운 형태 그대로 정보를 저장할 수 있다는 점이다,





그외의 MongDB의 장단점에 대해서는 아래 링크로 대신한다.


토비의 스프링을 살펴보면서 나온 트랜잭션이라는 용어와 함께 

트랜잭션에서 쓰이는 ACID에 대해서 따로 글을 써놓으려 한다.



트랜잭션이란 더 이상 나눌 수 없는 단위 작업을 뜻한다.

사용자가 시스템에 요구를 시작하여 시스템 내의 처리
시스템에서 사용자에게 응답하는 모든 처리를 포함한다. 

이러한 트랜잭션이 충족해야 하는 기술적인 요건은 ACID가 있다. 

1) 원자성 : 트랜잭션의 처리는 완전히 끝마치지 않았을 경우에는 
                  전혀 이루어지지 않은 것과 같아야 한다. 
2) 일관성 : 트랜잭션들간의 영향이 한 방향으로만 전달되어야 한다. 
3) 고립성 : 트랜잭션의 부분적인 상태를 다른 트랜잭션에 제공해서는 안된다. 
4) 지속성 : 성공적인 트랜잭션의 수행 후에는 반드시 데이터베이스에 반영되어야 한다.



조금 더 자세한 설명은 아래에 첨부하였으며 위키백과를 참조하였다.



ACID(원자성, 일관성, 고립성, 지속성)는 데이터베이스 트랜잭션이 안전하게 수행된다는 것을 보장하기 위한 성질을 가리키는 약어이다. 



데이터베이스에서 데이터에 대한 하나의 논리적 실행단계를 트랜잭션이라고 한다. 



예를 들어, 은행에서의 계좌이체를 트랜잭션이라고 할 수 있는데, 계좌이체 자체의 구현은 내부적으로 여러 단계로 이루어질 수 있지만 

전체적으로는 '송신자 계좌의 금액 감소', '수신자 계좌의 금액 증가'가 한 동작으로 이루어져야 하는 것을 의미한다.



원자성(Atomicity)은 트랜잭션과 관련된 작업들이 부분적으로 실행되다가 중단되지 않는 것을 보장하는 능력이다. 

예를 들어, 자금 이체는 성공할 수도 실패할 수도 있지만 보내는 쪽에서 돈을 빼 오는 작업만 성공하고 받는 쪽에 돈을 넣는 작업을 실패해서는 안된다. 원자성은 이와 같이 중간 단계까지 실행되고 실패하는 일이 없도록 하는 것이다.



일관성(Consistency)은 트랜잭션이 실행을 성공적으로 완료하면 언제나 일관성 있는 데이터베이스 상태로 유지하는 것을 의미한다. 

무결성 제약이 모든 계좌는 잔고가 있어야 한다면 이를 위반하는 트랜잭션은 중단된다.



고립성(Isolation)은 트랜잭션을 수행 시 다른 트랜잭션의 연산 작업이 끼어들지 못하도록 보장하는 것을 의미한다. 

이것은 트랜잭션 밖에 있는 어떤 연산도 중간 단계의 데이터를 볼 수 없음을 의미한다. 은행 관리자는 이체 작업을 하는 도중에 쿼리를 실행하더라도 특정 계좌간 이체하는 양 쪽을 볼 수 없다. 공식적으로 고립성은 트랜잭션 실행내역은 연속적이어야 함을 의미한다. 성능관련 이유로 인해 이 특성은 가장 유연성 있는 제약 조건이다. 자세한 내용은 관련 문서를 참조해야 한다.



지속성(Durability)은 성공적으로 수행된 트랜잭션은 영원히 반영되어야 함을 의미한다. 

시스템 문제, DB 일관성 체크 등을 하더라도 유지되어야 함을 의미한다. 전형적으로 모든 트랜잭션은 로그로 남고 시스템 장애 발생 전 상태로 되돌릴 수 있다. 트랜잭션은 로그에 모든 것이 저장된 후에만 commit 상태로 간주될 수 있다.
