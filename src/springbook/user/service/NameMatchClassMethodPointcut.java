package springbook.user.service;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.util.PatternMatchUtils;

public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {

    // 모든 클래스를 다 허용하던 디폴트 클래스 필터를 프로퍼티로 받은 클래스 이름을 이용해서 필터를 만들어 덮어씌운다.
    public void setMappedClassName(String mappedClassName){
        this.setClassFilter(new ClassFilter() {
            @Override
            public boolean matches(Class<?> clazz) {
                return PatternMatchUtils.simpleMatch(mappedClassName, clazz.getSimpleName());
            }
        });
    }
}
