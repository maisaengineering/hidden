package droid.samepinch.co.app.helpers.module;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import droid.samepinch.co.data.dto.User;
import droid.samepinch.co.rest.ReqPosts;

/**
 * Created by cbenjaram on 7/9/15.
 */
@Module
public class DataModule {
    public DataModule() {
    }

    @Provides ReqPosts provideReqPosts(){
        return new ReqPosts();
    }

    @Provides @Singleton User provideAnonymousDot(){
        String anonymousStr = "anonymous";
        User anonymous = new User();
        anonymous.setUid(String.valueOf(0));
        anonymous.setFname(anonymousStr);
        anonymous.setLname(StringUtils.EMPTY);
        anonymous.setPrefName(anonymousStr);
        anonymous.setPinchHandle(anonymousStr);
        return anonymous;
    }
}
