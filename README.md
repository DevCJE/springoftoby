■ 자동 프록시 생성

투명한 부가기능을 적용하는 과정에서 발견됐던 대부분의 문제는 해결되었다. 타깃 코드는 여전히 깔끔한 채로 남아 있고, 부가기능은 한 번만 만들어 모든 타깃과 메소드에 재사용이 가능하고, 타깃의 적용 메소드를 선정하는 방식도 독립적으로 작성할 수 있도록 분리되어 있다.

하지만 아직 한 가지 해결할 과제가 남아 있다. 부가기능의 적용이 필요한 타깃 오브젝트마다 거의 비슷한 내용의 ProxyFactoryBean 빈 설정정보를 추가해주는 부분이다. 새로운 타깃이등장했다고 해서 코드를 손댈 필요는 없어졌지만, 설정은 매번 복사해서 붙이고 target 프로퍼티의 내용을 수정해줘야 한다. 이 경우 target 프로퍼티를 제외하면 빈 클래스의 종류, 어드바이스, 포인트컷의 설정이 동일하다.


■ 중복 문제의 접근 방법

지금까지 다뤄봤던 반복적이고 기계적인 코드에 대한 해결책을 생각해보자.
JDBC API를 사용하는 DAO 코드의 경우 바뀌지 않는 부분과 바뀌는 부분을 구분해서 분리하고, 템플릿과 콜백, 클라이언트로 나누는 방법을 통해 깔끔하게 해결했다. 전략 패턴과 DI를 적용한 덕분이다. 그런데 이와는 좀 다른 방법으로 반복되는 코드의 문제를 해결했던 것이 있다. 바로 반복적인 위임 코드가 필요한 프록시 클래스 코드다. 타깃 오브젝트로의 위임 코드와 부가기능 적용을 위한 코드가 프록시가 구현해야 하는 모든 인터페이스 메소드마다 반복적으로 필요했다. 이는 단순한 분리와 DI와는 다른 독특한 방법으로 해결했다. 다이내믹 프록시라는 런타임 코드 자동생성 기법을 이용한 것이다. 변하지 않는 타깃으로의 위임과 부가기능 적용 여부 판단이라는 부분은 코드 생성 기법을 이용하는 다이내믹 프록시 기술에 맡기고, 변하는 부가기능 코드는 별도로 만들어서 다이내믹 프록시 생성 팩토리에 DI로 제공하는 방법을 사용한 것이다. 의미있는 부가기능 로직인 트랜잭션 경계설정은 코드로 만들게 하고, 기계적인 코드인 타깃 인터페이스 구현과 위임, 부가기능 연동 부분은 자동생성하게 한 것이다.

반복적인 프록시의 메소드 구현을 코드 자동생성 기법을 이용해 해결했다면 반복적인 ProxyFactoryBean 설정 문제는 설정 자동등록 기법으로 해결할 수 없을까? 또는 실제 빈 오브젝트가 되는 것은 ProxyFactoryBean을 통해 생성되는 프록시 그 자체이므로 프록시가 자동으로 빈으로 생성되게 할 수는 없을까? 마치 다이내믹 프록시가 인터페이스만 제공하면 모든 메소드에 대한 구현 클래스를 자동으로 만들듯이, 일정한 타깃 빈의 목록을 제공하면 자동으로 각 타깃 빈에 대한 프록시를 만들어주는 방법이 있다면 ProxyFactoryBean 타입 빈 설정을 매번 추가해서 프록시를 만들어내는 수고를 덜 수 있을 것 같다.


■ 빈 후처리기를 이용한 자동 프록시 생성기

스프링은 컨테이너로서 제공하는 기능 중에서 변하지 않는 핵심적인 부분외에는 대부분 확장할 수 있도록 확장 포인트를 제공해준다. 그 중에서 관심을 가질 만한 확장 포인트는 바로 BeanPostProcessor라는 인터페이스를 구현해서 만드는 빈 후처리기이다. 빈 후처리기는 이름 그대로 스프링 빈 오브젝트로 만들어지고 난 후에, 빈 오브젝트를 다시 가공할 수 있게 해준다. 여기서는 그 빈 후처리기중 하나인 DefaultAdvisorAutoProxyCreator를 살펴보겠다. 이름에서 알 수 있듯이 어드바이저를 이용한 자동 프록시 생성기다.

빈 후처리기를 스프링에 적용하는 방법은 빈 후처리기 자체를 빈으로 등록하는 것이다. 스프링은 빈 후처리기가 빈으로 등록되어 있으면 빈 오브젝트가 생성될 때마다 빈 후처리기에 보내서 후처리 작업을 요청한다. 빈 후처리기는 빈 오브젝트의 프로퍼티를 강제로 수정할 수도 있고 별도의 초기화 작업을 수행할 수도 있다. 심지어는 만들어진 빈 오브젝트 자체를 바꿔치기 할 수도 있다. 따라서 스프링이 설정을 참고해서 만든 오브젝트가 아닌 다른 오브젝트를 빈으로 등록시키는 것이 가능하다. 이를 잘 이용하면 스프링이 생성하는 빈 오브젝트의 일부를 프록시로 포장하고, 프록시를 빈으로 대신 등록할 수도 있다. 바로 이것이 자동 프록시 생성 빈 후처리기이다.

DefaultAdvisorAutoProxyCreator 빈 후처리기가 등록되어 있으면 스프링은 빈 오브젝트를 만들 때마다 후처리기에게 빈을 보낸다. DefaultAdvisorAutoProxyCreator는 빈으로 등록된 모든 어드바이저 내의 포인트컷을 이용해 전달받은 빈이 프록시 적용 대상인지 확인한다. 프록시 적용대상이면 그때는 내장된 프록시 생성기에게 현재 빈에 대한 프록시를 만들게 하고, 만들어진 프록시에 어드바이저를 연결해준다. 빈 후처리기는 프록시가 생성되면 원래 컨테이너가 전달해준 빈 오브젝트 대신 프록시 오브젝트를 컨테이너에게 돌려준다. 컨테이너는 최종적으로 빈 후처리기가 돌려준 오브젝트를 빈으로 등록하고 사용한다. 적용할 빈을 선정하는 로직이 추가된 포인트컷이 담긴 어드바이저를 등록하고 빈 후처리기를 사용하면 일일이 ProxyFactoryBean 빈을 등록하지 않아도 타깃 오브젝트에 자동으로 프록시가 적용되게 할 수 있다. 마지막 남은 번거로운 ProxyFactoryBean 설정 문제를 말끔하게 해결해주는 놀라운 방법이다.


■ 확장된 포인트컷

지금까지 포인트컷이란 타깃 오브젝트의 메소드 중에서 어떤 메소드에 부가기능을 적용할지를 선정해주는 역할을 하였지만 사실 포인트컷은 어떤 빈에 프록시를 적용할지 선택하는 기능도 담겨 있다.
```
public interface Pointcut{
    // 프록시를 적용할 클래스인지 확인해준다.
    ClassFilter getClassFilter();
    // 어드바이스를 적용할 메소드인지 확인해준다.
    MethodMatcher getMethodMatcher();
}
```
이전에 우리가 사용했던 NameMatchMethodPointcut은 메소드 선별 기능만 가진 특별한 포인트컷이다. 메소드만 선별한다는 건 클래스 필터는 모든 클래스를 다 받아주도록 만들어져 있다는 뜻이다. 따라서 클래스의 종류는 상관없이 메소드만 판별한다. 어차피 ProxyFactoryBean에서 포인트컷을 사용할 때는 이미 타깃이 정해져 있기 때문에 포인트 컷은 메소드 선별만 해주면 그만이었다. 만약 Pointcut 선정 기능을 모두 적용한다면 먼저 프록시를 적용할 클래스인지 판단하고 나서, 적용 대상 클래스인 경우에는 어드바이스를 적용할 메소드인지 확인하는 식으로 동작한다.

ProxyFactoryBean에서는 굳이 클래스 레벨의 필터는 필요 없었지만, 모든 빈에 대해 프록시 자동 적용 대상을 선별해야 하는 빈 후처리기인 DefaultAdivsorAutoProxyCreator는 클래스와 메소드 선정 알고리즘을 모두 갖고 있는 포인트 컷이 필요하다.


■ 포인트컷 테스트

이제 포인트컷을 확장해서 클래스를 판별하고 이후에 메소드 선정 알고리즘을 적용하게 해보자.
```
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
```
포인트컷은 NameMatchMethodPointcut을 내부 익명 클래스 방식으로 확장해서 만들었다. 원래 모든 클래스를 다 받아주는 클래스 필터를 리턴하던 getClassFilter()를 오버라이드해서 이름이 HelloT로 시작하는 클래스만을 선정해주는 필터로 만들었다. 메소드 이름 선정기준은 기존에 사용한 것을 그대로 유지했다.

테스트는 세 가지 클래스에 대해 진행한다. 모두 기존에 있던 HelloTarget이라는 클래스를 그대로 상속한 것이라 메소드 내용은 동일하다. 단지 클래스 이름만 다를 뿐이다. 이 세 개의 클래스에 모두 동일한 포인트컷을 적용했다. 메소드 선정기준으로만 보자면 두 개의 메소드에는 어드바이스를 적용하고 마지막 것은 적용되지 않으면 된다. 하지만 두 번째 HelloWorld라는 클래스는 클래스 필터에서 이미 탈락해버리기 때문에 메소드 이름과 무관하게 모든 메소드가 어드바이스 적용 대상에서 제외된다.

포인트컷이 클래스 필터까지 동작해서 클래스를 걸러버리면 아무리 프록시를 적용했다고 해도 부가기능은 전혀 제공되지 않는다는 점에 주의해야 한다. 사실 클래스 필터에서 통과하지 못한 대상은 프록시를 만들 필요조차 없다. 어차피 어떤 메소드에도 부가기능이 적용되지 않을 텐데 굳이 프록시를 둘 이유가 없기 때문이다.


■ DefaultAdvisorAutoProxyCreator의 적용

프록시 자동생성 방식에서 사용할 포인트컷을 만드는 방법을 학습 테스트를 만들어가면서 살펴봤으니, 이제 실제로 적용할 차례다.

만들어야 할 클래스는 하나뿐이다. 포인트컷으로 이용했던 NameMatchMethodPointcut을 상속해서 프로퍼티로 주어진 이름 패턴을 가지고 클래스 이름을 비교하는 ClassFilter를 추가하도록 만들 것이다.
```
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
```
이제 적용할 자동 프록시 생성기인 DefaultAdvisorAutoProxyCreator는 등록된 빈 중에서 Advisor 인터페이스를 구현한 것을 모두 찾는다. 그리고 생성되는 모든 빈에 대해 어드바이저의 포인트컷을 적용해보면서 프록시 적용 대상을 선정한다. 빈 클래스가 프록시 선정 대상이라면 프록시를 만들어 원래 빈 오브젝트와 바꿔치기한다. 원래 빈 오브젝트는 프록시 뒤에 연결돼서 프록시를 통해서만 접근 가능하게 바뀌는 것이다. 따라서 타깃 빈에 의존한다고 정의한 다른 빈들은 프록시 오브젝트를 대신 DI 받게 될 것이다.

이제 자동 프록시 생성기를 스프링 빈으로 등록하면 된다. 이때에 id 어트리뷰트는 해당 빈이 참조되거나 조회될 필요가 없기에 넣지 않아도 무방하다. 이후 포인트컷을 등록한다.
```
<bean id ="transactionPointcut" class="springbook.user.service.NameMatchClassMethodPointcut">
		<property name="mappedClassName" value="*ServiceImpl"/>
		<property name="mappedName" value="upgrade*"/>
</bean>
```
어드바이스인 transactionAdvice 빈의 설정은 수정할 게 없다. 어드바이저 또한 마찬가지이다. 하지만 어드바이저로서 사용되는 방법이 바뀌었다는 사실은 기억해두자. ProxyFactoryBean으로 등록한 빈에서 명시적으로 DI 하는 빈은 존재하지 않고 대신 어드바이저를 이용하는 자동 프록시 생성기인 DefaultAdvisorAutoProxyCreator에 의해 자동수집되고, 프록시 대상 선정 과정에 참여하며, 자동생성된 프록시에 다이내믹하게 DI 돼서 동작하는 어드바이저가 된다.

자동 프록시 생성기를 사용함으로써 기존 테스트에 문제가 발생하는데 타깃을 테스트용 클래스로 바꿔치기 하는 방법을 사용했지만 이제는 자동 생성기에서 프록시만을 남겨줬을뿐 ProxyFactoryBean 같은게 남아 있지 않기에 더 이상 이 방법은 불가하다. 이제 자동 프록시 생성기라는 스프링 컨테이너에 종속적인 기법을 사용했기 때문에 예외상황을 위한 테스트 대상도 빈으로 등록해 줄 필요가 있다. 이제는 타깃을 코드에서 바꿔치기할 방법도 없을뿐더러, 자동 프록시 생성기의 적용이 되는지도 빈을 통해 확인할 필요가 있기 때문이다.
```
// 포인트컷의 클래스 필터에 선정되도록 이름 변경. 포인트컷은 *ServiceImpl 클래스들로 설정해놨다.
	static class TestUserServiceImpl extends UserServiceImpl {
		private String id = "madnite1"; // 테스트 픽스처의 값을 이제 가져올 수 없기에 고정

		protected void upgradeLevel(User user) {
			if (user.getId().equals(this.id)) throw new TestUserServiceException();
			super.upgradeLevel(user);
		}
	}
```
이제 TestUserServiceImpl을 빈으로 등록하자.
```
<bean id="testUserService" 
      class="springbook.user.service.UserServiceTest$TestUserServiceImpl" 
      parent="userService" />
```