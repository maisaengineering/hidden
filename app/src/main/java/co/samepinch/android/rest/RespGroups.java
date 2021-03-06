package co.samepinch.android.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by imaginationcoder on 7/1/15.
 */
public class RespGroups extends RestBase<RespGroups.Body> {
    public RespGroups() {
    }

    @Override
    public Body getBody() {
        return super.getBody();
    }

    @Override
    public void setBody(Body body) {
        super.setBody(body);
    }

    public static class Body {
        @SerializedName("uid")
        @Expose
        String uid;

        @SerializedName("posts_count")
        @Expose
        Integer postsCount;

        @SerializedName("followers_count")
        @Expose
        Integer followersCount;

        @SerializedName("image")
        @Expose
        String image;

        @SerializedName("name")
        @Expose
        String name;

        public Body() {

        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public Integer getPostsCount() {
            return postsCount;
        }

        public void setPostsCount(Integer postsCount) {
            this.postsCount = postsCount;
        }

        public Integer getFollowersCount() {
            return followersCount;
        }

        public void setFollowersCount(Integer followersCount) {
            this.followersCount = followersCount;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
