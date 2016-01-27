package fr.ecp.sio.appenginedemo.api;

import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.Message;
import fr.ecp.sio.appenginedemo.model.User;
import fr.ecp.sio.appenginedemo.utils.GCSUtils;
import fr.ecp.sio.appenginedemo.utils.MD5Utils;
import fr.ecp.sio.appenginedemo.utils.ValidationUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import static fr.ecp.sio.appenginedemo.data.UsersRepository.*;
import static fr.ecp.sio.appenginedemo.utils.GCSUtils.writeToFile;

/**
 * A servlet to handle all the requests on a specific user
 * All requests with path matching "/users/*" where * is the id of the user are handled here.
 */
public class UserServlet extends JsonServlet {

    // GCS bucket name for avatar storage
    private static final String BUCKET_NAME = "AvatarBucket";

    // A GET request should simply return the user
    @Override
    protected User doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Extract the id of the user from the last part of the path of the request
        // TODO: Check if this id is syntactically correct

        // The URL looks like /users/
        if (req.getPathInfo().replace("/","").isEmpty())
            throw new ApiException(400, "BadRequest", "You must specify an id or me");

        // The URL looks like /users/{id|me}/messages


        // Validate id and get user requested
        User userRequested = getUserFromReqInfoPath(req);

        // Check if the user wants his own details
        User authorizedUser = getAuthenticatedUser(req);

        if (authorizedUser == userRequested) {
            return authorizedUser;
        }

        // User want to see another one, we hide private data.
        userRequested.email="********";
        userRequested.password="********";

        return userRequested;
        // TODO: Not found?
        // TODO: Add some mechanism to hide private info about a user (email) except if he is the caller
    }

    // A POST request could be used by a user to edit its own account
    // It could also handle relationship parameters like "followed=true"
    @Override
    protected User doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Get the user as below
        // TODO: Apply some changes on the user (after checking for the connected user)
        // TODO: Handle special parameters like "followed=true" to create or destroy relationships

        boolean followed;

        // Get me
        User me = getAuthenticatedUser(req);

        // Check whether or not the user is authenticated
        if (me == null)
            throw new ApiException(401,"NotAllowed","Missing authentication token");

        // if reqPathInfo is empty, the path look like /users/
        // Not allowed
        if (req.getPathInfo().replace("/","").isEmpty())
            throw new ApiException(400, "BadRequest", "You must specify an id or me");


        // Get the user with id in requestInfoPath
        User user= getUserFromReqInfoPath(req);

        // We have the current user and the user he want to follow/unfollow or just edit
        // Check whether or not the user is trying to edit an account
        // We ignore followed parameter
        // we assume he cannot edit somebody else account.
        if (me == user) {
            // He is trying to edit his own account
            // get the request body
            // CAUTION : user can try to modify his id
            user = getJsonRequestBody(req,User.class);
            user.id = me.id;

            // Perform all the usual checking
            if (!ValidationUtils.validateLogin(user.login)) {
                throw new ApiException(400, "invalidLogin", "Login did not match the specs");
            }
            if (!ValidationUtils.validatePassword(user.password)) {
                throw new ApiException(400, "invalidPassword", "Password did not match the specs");
            }
            if (!ValidationUtils.validateEmail(user.email)) {
                throw new ApiException(400, "invalidEmail", "Invalid email");
            }

            if (UsersRepository.getUserByLogin(user.login) != me) {
                throw new ApiException(400, "duplicateLogin", "Duplicate login");
            }
            if (UsersRepository.getUserByEmail(user.email) != me) {
                throw new ApiException(400, "duplicateEmail", "Duplicate email");
            }


            // save avatar the new avatar
            if (!me.coverPicture.equalsIgnoreCase(user.coverPicture)){
                // Simulate an avatar image using Gravatar API
                user.avatar = "http://www.gravatar.com/avatar/" + MD5Utils.md5Hex(user.email) + "?d=wavatar";

                try {
                    GcsFilename fileName = new GcsFilename(BUCKET_NAME, user.login);
                    GcsFileOptions options = new GcsFileOptions.Builder()
                            .mimeType("image/jpeg")
                            .acl("public-read")
                            .addUserMetadata("user", user.login)
                            .build();
                    // Download picture from gravatar.com
                    URL url = new URL(user.avatar);
                    byte[] content = IOUtils.toByteArray(url);

                    // Save picture to GCS
                    writeToFile(fileName, options, content);

                    // Get picture URL
                    ServingUrlOptions urlOptions = ServingUrlOptions
                            .Builder.withGoogleStorageFileName("/gs/" + BUCKET_NAME + "/" + user.login);
                    ImagesService imagesService = ImagesServiceFactory.getImagesService();

                    // Set coverPicture to picture URL
                    user.coverPicture = imagesService.getServingUrl(urlOptions);

                    // Delete old avatar
                    GCSUtils.deleteFile(new GcsFilename(BUCKET_NAME,me.login));

                } catch (Exception e){
                    throw new ApiException(500,"InternalServerError","Internal Server Error");
                }
            }
            saveUser(user);
            return getUser(me.id);
        }

        // Check followed parameter
        if (req.getHeader("followed") != null) {
            try {
                followed = Boolean.parseBoolean(req.getHeader("followed"));
                // Set current user to follow/unfollow the user with given id
                // At this point me is always different from user
                // it means that user cannot follow/unfollow himself
                setUserFollowed(me.id,user.id,followed);
            }
            catch (Exception e){
                // followed value cannot be parsed to boolean
                throw new ApiException(400,"BadRequest","followed must be set to true or false");
            }
        }

        // TODO: Return the modified user
        return getUser(me.id);
    }

    // A user can DELETE its own account
    @Override
    protected Void doDelete(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Security checks
        // TODO: Delete the user, the messages, the relationships
        // A DELETE request shall not have a response body

        if (req.getPathInfo().replace("/","").isEmpty())
            throw new ApiException(400, "BadRequest", "You must specify your id or me");

        // Get the current user
        User me = getAuthenticatedUser(req);

        // Check whether or not the user is authenticated
        if (me == null)
            throw new ApiException(401,"MethodNotAllowed","Missing authorization token");

        // Get the user that is going to be deleted
        User user = getUserFromReqInfoPath(req);

        // Delete the user
        if (me == user){
            // Delete all messages of the user
            // Iterate through messages to get his messages
            Iterator <Message> messageIterator = MessagesRepository.getMessages().listIterator();
            while (messageIterator.hasNext()){
                if (messageIterator.next().user.getValue()==me)
                    // delete the message
                    MessagesRepository.deleteMessage(messageIterator.next().id);
            }

            // Destroy relationships
            // Followers
            Iterator<User> userIterator = UsersRepository.getUserFollowers(me.id,null,null).users.listIterator();
            while (userIterator.hasNext()){
                UsersRepository.setUserFollowed(userIterator.next().id,me.id,false);
            }

            // Followed
            userIterator = UsersRepository.getUserFollowed(me.id,null,null).users.listIterator();
            while (userIterator.hasNext()){
                UsersRepository.setUserFollowed(me.id,userIterator.next().id,false);
            }

            // delete the user
            UsersRepository.deleteUser(me.id);

            // delete avatar

            try {
                GcsFilename fileName = new GcsFilename(BUCKET_NAME, user.login);
                GCSUtils.deleteFile(fileName);
            } catch (Exception e) {
                throw new ApiException(500,"InternalServerError","Internal Server Error");
            }


        }
        else
            // The user is trying to delete another user
            throw new ApiException(401,"NotAllowed","Not permitted");

        return null;
    }


    private User getUserFromReqInfoPath(HttpServletRequest request) throws NumberFormatException, ApiException {
        // Hold the id
        String requestPathInfo = request.getPathInfo().replace("/","");
        long id;
        User user;

        if (requestPathInfo.equalsIgnoreCase("me")){
            user = getAuthenticatedUser(request);
            if (user==null)
                throw new ApiException(401,"NotAllowed","Missing authorization token");
        } else {
            try {
                id = Long.parseLong(requestPathInfo);
                user = getUser(id);
                if (user==null)
                    throw new ApiException(404, "NotFound", "No user have that id");
            }
            catch (NumberFormatException e){
                throw new ApiException(400, "BadRequest", "You must specify an id or me");
            }
        }
        return user;
    }
}