package de.doridian.ccftp;


import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

import adamros.ccftp.BaseCCFTP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CCUserManager implements UserManager {
    private final long max_filesize;
    private final static Pattern filesizePat = Pattern.compile("([0-9]+)(KB|MB|GB|B)?");

    public CCUserManager(String maxFilesize) throws FtpException {
        Matcher matcher = filesizePat.matcher(maxFilesize.toUpperCase());
        if(!matcher.matches()) throw new FtpException("Invalid maximum filesize");
        final long tempRawSize = Long.valueOf(matcher.group(1));
        if(matcher.groupCount() > 1) {
            final String tempRawUnit = matcher.group(2);
            if(tempRawUnit == null || tempRawUnit.isEmpty() || tempRawUnit.equals("B")) {
                max_filesize = tempRawSize;
            } else if(tempRawUnit.equals("KB")) {
                max_filesize = tempRawSize * 1024;
            } else if(tempRawUnit.equals("MB")) {
                max_filesize = tempRawSize * 1024 * 1024;
            } else if(tempRawUnit.equals("GB")) {
                max_filesize = tempRawSize * 1024 * 1024 * 1024;
            } else {
                throw new FtpException("Invalid unit");
            }
        } else {
            max_filesize = tempRawSize;
        }

        System.out.println("Set max filesize to be " + max_filesize + " bytes");
    }
    
	Pattern userPat = Pattern.compile("((.*)\\.)?([0-9]+)");
	@Override
	public User getUserByName(String username) throws FtpException {
		Matcher matcher = userPat.matcher(username);
		if(!matcher.matches()) return null;
		return new CCUser(matcher.group(2), Integer.valueOf(matcher.group(3)));
	}

	@Override
	public User authenticate(Authentication authentication) throws AuthenticationFailedException {
		if(authentication instanceof AnonymousAuthentication) {
			throw new AuthenticationFailedException("Anonymous login not permitted");
		} else if(authentication instanceof UsernamePasswordAuthentication) {
			UsernamePasswordAuthentication upwd = (UsernamePasswordAuthentication)authentication;
			try {
				User user = getUserByName(upwd.getUsername());
				if(user == null) throw new AuthenticationFailedException("Invalid username (its either ID or WORLD.ID)");
				String cpwd = user.getPassword();
				if(cpwd != null && !cpwd.isEmpty() && cpwd.equals(upwd.getPassword())) {
					return user;
				}
			} catch(AuthenticationFailedException e) {
				throw e;
			} catch(FtpException e) {
				e.printStackTrace();
			}
			throw new AuthenticationFailedException("Authentication failed (did you remember to use WORLD.ID if the computer is not in the default world?)");
		} else {
			throw new AuthenticationFailedException("Authentication method not supported");
		}
	}

	private class CCUser implements User {
		private final String username;
		private final File folder;
		private String password;

		private CCUser(String worldName, int computerID) {
			if(worldName == null || worldName.isEmpty()) {
				//Forge port change
				worldName = BaseCCFTP.instance.defaultWorld;
			}

			this.username = worldName + "." + computerID;
			this.folder = new File(new File(new File(worldName), "computer"), String.valueOf(computerID));

			try {
				BufferedReader reader = new BufferedReader(new FileReader(new File(folder, "_ftppasswd")));
				password = reader.readLine();
				reader.close();
			} catch(Exception e) { password = null; }
		}
		
		@Override
		public String getName() {
			return username;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public List<Authority> getAuthorities() {
			return null;
		}

		@Override
		public List<Authority> getAuthorities(Class<? extends Authority> aClass) {
			return null;
		}

		@Override
		public AuthorizationRequest authorize(AuthorizationRequest authorizationRequest) {
            if(authorizationRequest instanceof MaxFilesizeRequest) {
                ((MaxFilesizeRequest)authorizationRequest).setMaxFilesize(max_filesize);
            }
			return authorizationRequest;
		}

		@Override
		public int getMaxIdleTime() {
			return 0;
		}

		@Override
		public boolean getEnabled() {
			return true;
		}

		@Override
		public String getHomeDirectory() {
			return folder.getAbsolutePath();
		}
	}

	@Override
	public String[] getAllUserNames() throws FtpException {
		return new String[0];
	}

	@Override
	public boolean doesExist(String username) throws FtpException {
		return true;
	}

	@Override
	public String getAdminName() throws FtpException {
		return null;
	}

	@Override
	public boolean isAdmin(String s) throws FtpException {
		return false;
	}

	@Override
	public void delete(String s) throws FtpException { }

	@Override
	public void save(User user) throws FtpException { }
}
