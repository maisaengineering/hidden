package co.samepinch.android.rest;

/**
 * Created by imaginationcoder on 6/30/15.
 */
public class RespEvaluateUser extends RestBase<RespEvaluateUser.Body> {
    public RespEvaluateUser() {
    }


    public static class Body {
        boolean newUser;

        public Body() {
        }

        public boolean isNewUser() {
            return newUser;
        }

        public void setNewUser(boolean newUser) {
            this.newUser = newUser;
        }
    }
}
