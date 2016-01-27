package fr.ecp.sio.appenginedemo.api;


import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.User;
import fr.ecp.sio.appenginedemo.utils.MD5Utils;
import fr.ecp.sio.appenginedemo.utils.TokenUtils;
import fr.ecp.sio.appenginedemo.utils.ValidationUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static fr.ecp.sio.appenginedemo.data.UsersRepository.getUserFollowed;
import static fr.ecp.sio.appenginedemo.data.UsersRepository.getUserFollowers;
import static fr.ecp.sio.appenginedemo.utils.GCSUtils.writeToFile;

/**
 * A servlet to handle all the requests on a list of users
 * All requests on the exact path "/users" are handled here.
 */
public class UsersServlet extends JsonServlet {
    // GCS bucket name for avatar storage
    private static final String BUCKET_NAME = "AvatarBucket";

    // A GET request should return a list of users
    @Override
    protected List<User> doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: define parameters to search/filter users by login, with limit, order...
        // TODO: define parameters to get the followings and the followers of a user given its id

        // paramValue holds the id retrieved from the request as string
        String paramValue = "";

        // Hold followedBy, followerOf or empty string
        String paramKey = "";

        // id holds the id as long
        long id = 0;

        // We retrieve  the id and param key from the request
        if(req.getParameterMap().containsKey("followedBy")){
            paramValue = req.getParameter("followedBy");
            paramKey = "followedBy";
        } else if (req.getParameterMap().containsKey("followerOf")){
            paramValue = req.getParameter("followerOf");
            paramKey = "followerOf";
        }


        // We return all the users if paramValue is empty
        // The followers/followed parameter is not set
        if (paramValue.isEmpty()){
            // The request URL is /users All the users should be returned
            return  UsersRepository.getUsers(null,null).users;
        }

        // paramValue is set to some value. We retrieve the user id
        if (paramValue.equalsIgnoreCase("me")){
            try {
                id = getAuthenticatedUser(req).id;
            }
            catch (NullPointerException e){
                // The id is set to "me" but user Authorization token is missing
                throw new ApiException(401,"NotAllowed","Missing authorization token");
            }
        } else {
            try {
                // Trying to parse a string to long can produce NumberFormatException
                id = Long.parseLong(paramValue);
            }
            catch (Exception e){
                // The id is not "me" and cannot be parsed to long integer
                throw new  ApiException(400, "BadRequest", "You must specify an id or me");
            }
        }

        // We retrieve limit and cursor from parameters

        // limit holds value of limit
        int limit;
        try {
            limit = req.getIntHeader("limit");
        }
        catch (NumberFormatException e){
            throw new ApiException(400, "invalidRequest", "Invalid value of limit");
        }

        // If not set getIntHeader will return -1, we can't allow a negative value
        // for the limit, so we change it to the biggest int: Integer.MAX_VALUE
        if (limit <= 0)
            limit = Integer.MAX_VALUE;


        // cursor holds value of continuation token
        // We don't need to test cursor value since it is optional
        // if not set getHeader will return null
        String cursor = req.getHeader("cursor");



        // All information are gathered

        switch (paramKey){
            case "followedBy":
                return getUserFollowed(id,limit,cursor).users;
            case "followerOf":
                return getUserFollowers(id,limit,cursor).users;
        }

        return null;
    }

    // A POST request can be used to create a user
    // We can use it as a "register" endpoint; in this case we return a token to the client.
    @Override
    protected String doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {

        // The request should be a JSON object describing a new user
        User user = getJsonRequestBody(req, User.class);
        if (user == null) {
            throw new ApiException(400, "invalidRequest", "Invalid JSON body");
        }

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

        if (UsersRepository.getUserByLogin(user.login) != null) {
            throw new ApiException(400, "duplicateLogin", "Duplicate login");
        }
        if (UsersRepository.getUserByEmail(user.email) != null) {
            throw new ApiException(400, "duplicateEmail", "Duplicate email");
        }

        // Explicitly give a fresh id to the user (we need it for next step)
        user.id = UsersRepository.allocateNewId();

        // TODO: find a solution to receive an store profile pictures

        // Simulate an avatar image using Gravatar API
        user.avatar = "http://www.gravatar.com/avatar/" + MD5Utils.md5Hex(user.email) + "?d=wavatar";

        // Uploading avatar to Google Cloud Storage
        try {
            // Declare the filename
            GcsFilename filename = new GcsFilename(BUCKET_NAME, user.login);

            // Declare file options
            GcsFileOptions options = new GcsFileOptions.Builder()
                    .mimeType("image/jpeg")
                    .acl("public-read")
                    .addUserMetadata("user", user.login)
                    .build();

            // Download picture from gravatar.com
            URL url = new URL(user.avatar);
            byte[] content = IOUtils.toByteArray(url);

            // Save picture to GCS
            writeToFile(filename, options, content);

            // Build avatar url
            ServingUrlOptions urlOptions = ServingUrlOptions
                    .Builder.withGoogleStorageFileName("/gs/" + BUCKET_NAME + "/" + user.login);

            ImagesService imagesService = ImagesServiceFactory.getImagesService();

            // Set coverPicture to picture URL
            user.coverPicture = imagesService.getServingUrl(urlOptions);

        } catch (Exception e){
            throw new ApiException(500,"InternalServerError","Internal Server Error");
        }



        // Hash the user password with the id a a salt
        user.password = DigestUtils.sha256Hex(user.password + user.id);

        // Persist the user into the repository
        UsersRepository.saveUser(user);

        // Create and return a token for the new user
        return TokenUtils.generateToken(user.id);

    }

}
