package fr.ecp.sio.appenginedemo.api;

import com.googlecode.objectify.Ref;
import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.model.Message;
import fr.ecp.sio.appenginedemo.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A servlet to handle all the requests on a specific message
 * All requests with path matching "/messages/*" where * is the id of the message are handled here.
 */
public class MessageServlet extends JsonServlet {

    // A GET request should simply return the message
    @Override
    protected Message doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Extract the id of the message from the last part of the path of the request
        // TODO: Check if this id is syntactically correct
        Message message = getMessageFromReqInfoPath(req);
        // TODO: Not found?
        if (message==null)
            throw  new ApiException(404,"NotFound","Message not found");
        return message;
    }

    // A POST request could be made to modify some properties of a message after it is created
    @Override
    protected Message doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Get the message as below
        // TODO: Apply the changes
        // TODO: Return the modified message
        User user = getAuthenticatedUser(req);
        if (user == null)
            throw new ApiException(401,"NotAllowed","Missing authorization token");

        long messageId = getMessageIdFromReqInfoPath(req);

        // TODO: Not found?
        Message message = getJsonRequestBody(req,Message.class);
        // Override the new message id with the old one. this avoid manual id edition
        message.id = messageId;
        MessagesRepository.insertMessage(message);
        return MessagesRepository.getMessage(messageId);
    }

    // A DELETE request should delete a message (if the user)
    @Override
    protected Void doDelete(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Get the message
        // TODO: Check that the calling user is the author of the message (security!)
        // TODO: Delete the message
        User user = getAuthenticatedUser(req);
        if (user == null)
            throw new ApiException(401,"NotAllowed","Missing authorization token");

        Message message = getMessageFromReqInfoPath(req);

        // TODO: Not found?
        if (message.user.getValue()==user)
            MessagesRepository.deleteMessage(message.id);
        else
            throw new ApiException(401,"MethodNotAllowed","You cannot delete somebody else message");
        // A DELETE request shall not have a response body
        return null;
    }


    private Message getMessageFromReqInfoPath(HttpServletRequest request) throws NumberFormatException,ApiException {
        String requestPathInfo = request.getPathInfo().replace("/","");
        long id;

        try {
            id = Long.parseLong(requestPathInfo);
        }catch (Exception e){
            throw new ApiException(400,"BadRequest","The id must be an integer");
        }
        // Lookup in repository
        return MessagesRepository.getMessage(id);
    }

    private long getMessageIdFromReqInfoPath(HttpServletRequest request) throws NumberFormatException,ApiException {
        String requestPathInfo = request.getPathInfo().replace("/","");
        try {
            return Long.parseLong(requestPathInfo);
        }catch (Exception e){
            throw new ApiException(400,"BadRequest","The id must be an integer");
        }
    }
}
