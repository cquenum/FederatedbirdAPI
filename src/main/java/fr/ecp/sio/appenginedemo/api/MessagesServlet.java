package fr.ecp.sio.appenginedemo.api;

import com.googlecode.objectify.Ref;
import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.Message;
import fr.ecp.sio.appenginedemo.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A servlet to handle all the requests on a list of messages
 * All requests on the exact path "/messages" are handled here.
 */
public class MessagesServlet extends JsonServlet {

    // A GET request should return a list of messages
    @Override
    protected List<Message> doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: filter the messages that the user can see (security!)
        // TODO: filter the list based on some parameters (order, limit, scope...)

        // TODO: e.g. add a parameter to get the messages of a user given its id (i.e. /messages?author=256439)
        // paramValue holds the id retrieved from the request as string
        String paramValue = "";

        // id holds the id as long
        long id = 0;

        // We retrieve  the id and param key from the request
        if(req.getParameterMap().containsKey("author")){
            paramValue = req.getParameter("author");
        }


        // We return all the messages if paramValue is empty
        // The author parameter is not set
        if (paramValue.isEmpty()){
            // The request URL is /messages All the messages should be returned
            return MessagesRepository.getMessages();
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

        //return MessagesRepository.getMessages(UsersRepository.getUser(id));
        return MessagesRepository.getMessages(id);
    }

    // A POST request on a collection endpoint should create an entry and return it
    @Override
    protected Message doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {

        // Check user authorization
        User user = getAuthenticatedUser(req);
        if (user==null)
            throw new ApiException(401,"NotAllowed","Missing authorization token");

        // The request should be a JSON object describing a new message
        Message message = getJsonRequestBody(req, Message.class);
        if (message == null) {
            throw new ApiException(400, "invalidRequest", "Invalid JSON body");
        }

        // TODO: validate the message here (minimum length, etc.)

        // Some values of the Message should not be sent from the client app
        // Instead, we give them here explicit value
        message.user = Ref.create(user);
        message.date = new Date();
        message.id = null;

        // Our message is now ready to be persisted into our repository
        // After this call, our repository should have given it a non-null id
        MessagesRepository.insertMessage(message);

        return message;
    }

}
