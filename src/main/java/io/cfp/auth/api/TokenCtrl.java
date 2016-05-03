package io.cfp.auth.api;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.FileNotFoundException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.cfp.auth.dto.ErrorRes;
import io.cfp.auth.dto.LoginReq;
import io.cfp.auth.dto.LoginRes;
import io.cfp.auth.dto.Token;
import io.cfp.auth.entity.User;
import io.cfp.auth.repository.UserRepo;
import io.cfp.auth.service.TokenSrv;

/**
 * Login a user
 */
@RestController
@RequestMapping("/api/tokens")
public class TokenCtrl {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private TokenSrv tokenSrv;


	@RequestMapping(value = "", method = POST)
	public LoginRes login(@RequestBody LoginReq req, HttpServletResponse response) throws FileNotFoundException {
		User user = userRepo.findByEmail(req.getEmail());

		if (user == null) {
			throw new FileNotFoundException("User/Pass invalid");
		}

		if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
			throw new FileNotFoundException("User/Pass invalid");
		}

		String email = user.getEmail();

		//add a token for the user
		Token token = tokenSrv.create(email, null);

		LoginRes res = new LoginRes();
		res.setToken(token.getValue());

		Cookie tokenCookie = new Cookie("token", token.getValue());
		tokenCookie.setPath("/");
		tokenCookie.setHttpOnly(true); //secure Token to be invisible from javascript in the browser
		response.addCookie(tokenCookie);

		return res;
	}

	@RequestMapping(value = "/{token}", method = GET)
	public Token getToken(@PathVariable String token) throws FileNotFoundException {
		Token resToken = tokenSrv.get(token);
		if (resToken == null) {
			throw new FileNotFoundException("Token not found");
		}

		return resToken;
	}

	@RequestMapping(value = "/{token}", method = DELETE)
	@ResponseStatus(NO_CONTENT)
	public void logout(@PathVariable String token) throws FileNotFoundException {
		tokenSrv.remove(token);
	}

	@ExceptionHandler(FileNotFoundException.class)
	@ResponseStatus(NOT_FOUND)
	public ErrorRes handleFNFE(FileNotFoundException e) {
		return new ErrorRes("Not Found", e.getMessage());
	}

}
