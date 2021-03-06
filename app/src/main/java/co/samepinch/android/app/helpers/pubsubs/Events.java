package co.samepinch.android.app.helpers.pubsubs;

import java.util.Map;

/**
 * Created by cbenjaram on 7/15/15.
 */
public class Events {
    public static class EventBase {
        private final Map<String, String> metaData;

        public EventBase(Map<String, String> metaData) {
            this.metaData = metaData;
        }

        public Map<String, String> getMetaData() {
            return metaData;
        }
    }

    public static class PostsRefreshedEvent extends EventBase {
        public PostsRefreshedEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class FavPostsRefreshedEvent extends EventBase {
        public FavPostsRefreshedEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class TagRefreshedEvent extends EventBase {
        public TagRefreshedEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class TagsRefreshedEvent extends EventBase {
        public TagsRefreshedEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class TagsRefreshFailEvent extends EventBase {
        public TagsRefreshFailEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class PostDetailsRefreshEvent extends EventBase {
        public PostDetailsRefreshEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class PostDetailsRefreshFailEvent extends EventBase {
        public PostDetailsRefreshFailEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AuthSuccessEvent extends EventBase {
        public AuthSuccessEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AuthOutEvent extends EventBase {
        public AuthOutEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AuthOutFailEvent extends EventBase {
        public AuthOutFailEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AuthFailEvent extends EventBase {
        public AuthFailEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AuthAccExistsEvent extends EventBase {
        public AuthAccExistsEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AuthAccNotExistsEvent extends EventBase {
        public AuthAccNotExistsEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class SignUpSuccessEvent extends EventBase {
        public SignUpSuccessEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class SignUpFailEvent extends EventBase {
        public SignUpFailEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class MessageEvent extends EventBase {
        public MessageEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class SignUpImageUploadEvent extends EventBase {
        public SignUpImageUploadEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class MultiMediaUploadEvent extends EventBase {
        public MultiMediaUploadEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class CommentDetailsRefreshEvent extends EventBase {
        public CommentDetailsRefreshEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class CommentDetailsEditEvent extends EventBase {
        public CommentDetailsEditEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class DotDetailsRefreshEvent extends EventBase {
        public DotDetailsRefreshEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class DotDetailsRefreshFailEvent extends EventBase {
        public DotDetailsRefreshFailEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class PostMetaUpdateServiceSuccessEvent extends EventBase {
        public PostMetaUpdateServiceSuccessEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class PostMetaUpdateServiceFailEvent extends EventBase {
        public PostMetaUpdateServiceFailEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AllNotifsRefreshedEvent extends EventBase {
        public AllNotifsRefreshedEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AllNotifsUPDATEEvent extends EventBase {
        public AllNotifsUPDATEEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class AllNotifsERROREvent extends EventBase {
        public AllNotifsERROREvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class SingleNotifsUPDATEEvent extends EventBase {
        public SingleNotifsUPDATEEvent(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class IPCheckerServiceSuccess extends EventBase {
        public IPCheckerServiceSuccess(Map<String, String> metaData) {
            super(metaData);
        }
    }

    public static class IPCheckerServiceErr extends EventBase {
        public IPCheckerServiceErr(Map<String, String> metaData) {
            super(metaData);
        }
    }
}
