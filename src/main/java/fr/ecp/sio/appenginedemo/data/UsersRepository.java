package fr.ecp.sio.appenginedemo.data;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import fr.ecp.sio.appenginedemo.model.Deref;
import fr.ecp.sio.appenginedemo.model.Relationship;
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
        ObjectifyService.register(Relationship.class);
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
        long id = new ObjectifyFactory().allocateId(User.class).getId();
        Relationship r = new Relationship();
        r.user = Ref.create(Key.create(User.class, id));
        ObjectifyService.ofy().save().entity(r).now();
        return id;
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
        //return getUsers(limit, cursor);
        Relationship r = ObjectifyService.ofy()
                .load().type(Relationship.class)
                .ancestor(Key.create(User.class, id)).first().now();
        return new UsersList(Deref.deref(r.followed), cursor);
    }

    /**
     * @param id The id of the user
     * @param limit The maximum number of items to retrieve, optional
     * @param cursor Optional cursor to get the next items
     * @return A list of users with optionally a cursor
     */
    public static UsersList getUserFollowers(long id, Integer limit, String cursor) {
        //return getUsers(limit, cursor);
        Relationship r = ObjectifyService.ofy()
                .load().type(Relationship.class)
                .ancestor(Key.create(User.class, id)).first().now();
        return new UsersList(Deref.deref(r.followers), cursor);
    }

    /**
     * @param followerId The id of the follower
     * @param followedId The id of the followed
     * @param followed true to follow, false to unfollow
     */
    public static void setUserFollowed(long followerId, long followedId, boolean followed) {
        Ref<User> followerRef = Ref.create(Key.create(User.class, followerId));
        Ref<User> followedRef = Ref.create(Key.create(User.class, followedId));

        Relationship r_1 = ObjectifyService.ofy()
                .load().type(Relationship.class)
                .ancestor(Key.create(User.class, followerId)).first().now();
        Relationship r_2 = ObjectifyService.ofy()
                .load().type(Relationship.class)
                .ancestor(Key.create(User.class, followedId)).first().now();


        if (r_1.followed.contains(followedRef))
            return;

        if (followed) {
            r_1.followed.add(followedRef);
            r_2.followers.add(followerRef);
        } else {
            r_1.followed.remove(followedRef);
            r_2.followers.remove(followerRef);
        }

        // Commit changes to both relationship
        ObjectifyService.ofy().save().entity(r_1).now();
        ObjectifyService.ofy().save().entity(r_2).now();
    }

    public static boolean equals(User a, User b) {
        return Key.create(User.class, a.id).equivalent(Key.create(User.class, b.id));
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

}