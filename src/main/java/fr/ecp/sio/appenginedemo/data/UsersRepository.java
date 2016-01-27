package fr.ecp.sio.appenginedemo.data;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import fr.ecp.sio.appenginedemo.model.User;

import java.util.List;

/**
 * This is a repository class for the users.
 * It could be backed by any kind of persistent storage engine.
 * Here we use the Datastore from Google Cloud Platform, and we access it using the high-level Objectify library.
 */
public class UsersRepository {

    // A static initializer to register the model class with the Objectify service.
    // This is required per Objectify documentation.
    static {
        ObjectifyService.register(User.class);
    }

    public static User getUserByLogin(final String login) {
        // We can add filter of a property if this property has the @Index annotation in the model class
        // first() returns only one result
        return ObjectifyService.ofy()
                .load()
                .type(User.class)
                .filter("login", login)
                .first()
                .now();
    }

    public static User getUserByEmail(final String email) {
        return ObjectifyService.ofy()
                .load()
                .type(User.class)
                .filter("email", email)
                .first()
                .now();
    }

    public static User getUser(long id) {
        return ObjectifyService.ofy()
                .load()
                .type(User.class)
                .id(id)
                .now();
    }

    /**
     * Get all the users from the datastore (usage???)
     * @param limit The maximum number of items to retrieve, optional
     * @param cursor Optional cursor to get the next items
     * @return All users
     */
    public static UsersList getUsers(Integer limit, String cursor) {
        return new UsersList(
                ObjectifyService.ofy()
                        .load()
                        .type(User.class)
                        .list(),
                "dummyCursor"
        );
    }

    public static long allocateNewId() {
        // Sometime we need to allocate an id before persisting, the library allows it
        return new ObjectifyFactory().allocateId(User.class).getId();
    }

    /**
     * Persist a user into the datastore
     * @param user The user to save
     */
    public static void saveUser(User user) {
        user.id = ObjectifyService.ofy()
                .save()
                .entity(user)
                .now()
                .getId();
    }

    /**
     * @param id The id of the user to remove
     */
    public static void deleteUser(long id) {
        ObjectifyService.ofy()
                .delete()
                .type(User.class)
                .id(id)
                .now();
    }

    /**
     * @param id The id of the user
     * @param limit The maximum number of items to retrieve, optional
     * @param cursor Optional cursor to get the next items
     * @return A list of users with optionally a cursor
     */
    public static UsersList getUserFollowed(long id, Integer limit, String cursor) {
        return getUsers(limit, cursor);
    }

    /**
     * @param id The id of the user
     * @param limit The maximum number of items to retrieve, optional
     * @param cursor Optional cursor to get the next items
     * @return A list of users with optionally a cursor
     */
    public static UsersList getUserFollowers(long id, Integer limit, String cursor) {
        return getUsers(limit, cursor);
    }

    /**
     * A list of users, with optionally a cursor to get the next items
     */
    public static class UsersList {

        public final List<User> users;
        public final String cursor;

        private UsersList(List<User> users, String cursor) {
            this.users = users;
            this.cursor = cursor;
        }

    }

    /**
     * @param followerId The id of the follower
     * @param followedId The id of the followed
     * @param followed true to follow, false to unfollow
     */
    public static void setUserFollowed(long followerId, long followedId, boolean followed) {
        // Not implemented yet
    }

}