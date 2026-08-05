package com.booksocial.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class UserHeadersRequestWrapper extends HttpServletRequestWrapper {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";


    private final String userId;
    private final String userEmail;
    private final String userRoles;

    public UserHeadersRequestWrapper(HttpServletRequest request,
                                     String userId,
                                     String userEmail,
                                     String userRoles) {
        super(request);
        this.userId = userId;
        this.userEmail = userEmail;
        this.userRoles = userRoles;
    }

    @Override
    public String getHeader(String name) {
        if (USER_ID_HEADER.equalsIgnoreCase(name)) return this.userId;
        if (USER_EMAIL_HEADER.equalsIgnoreCase(name)) return this.userEmail;
        if (USER_ROLES_HEADER.equalsIgnoreCase(name)) return this.userRoles;

        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (USER_ID_HEADER.equalsIgnoreCase(name)
            || USER_EMAIL_HEADER.equalsIgnoreCase(name)
            || USER_ROLES_HEADER.equalsIgnoreCase(name))
        {
        String value = getHeader(name);
        return value != null
                ? Collections.enumeration(List.of(value))
                : Collections.emptyEnumeration();
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.removeIf(name -> USER_ID_HEADER.equalsIgnoreCase(name)
                || USER_EMAIL_HEADER.equalsIgnoreCase(name)
                || USER_ROLES_HEADER.equalsIgnoreCase(name));

        if (userId != null) names.add(USER_ID_HEADER);
        if (userEmail != null) names.add(USER_EMAIL_HEADER);
        if (userRoles != null) names.add(USER_ROLES_HEADER);

        return Collections.enumeration(names);
    }
}
