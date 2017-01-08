package org.ff4j.security;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.ff4j.utils.JsonUtils;

/**
 * Wrapper to manipulate grantees for permissions.
 *
 * @author Cedrick LUNVEN  (@clunven)
 */
public class FF4jGrantees {
    
    /** Usernames (unique identifier) for users. */
    private Set < String > users = new HashSet<>();

    /** Roles Names. */
    private Set < String > roles = new HashSet<>();
    
    /**
     * Default constructor
     */
    public FF4jGrantees() {
    }
    
    /**
     * Constructor will all parameters.
     *
     * @param groups
     *      target group name
     * @param users
     *      target user names
     */
    public FF4jGrantees(Set <String> users, Set < String > groups) {
        this.users  = users;
        this.roles = groups;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toJson();
    }
    
    /** {@inheritDoc} */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"users\":");
        sb.append(JsonUtils.collectionAsJson(users));
        sb.append(",\"roles\":");
        sb.append(JsonUtils.collectionAsJson(roles));
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Add dedicated user.
     *
     * @param group
     *      target group
     * @return
     */
    public FF4jGrantees grantUser(String user) {
        users.add(user);
        return this;
    }
    
    /**
     * Add dedicated user.
     *
     * @param group
     *      target group
     * @return
     */
    public FF4jGrantees revokeUser(String user) {
        users.remove(user);
        return this;
    }
    
    /**
     * Add dedicated group.
     *
     * @param group
     *      target group
     * @return
     */
    public FF4jGrantees grantRole(String group) {
        roles.add(group);
        return this;
    }
    
    /**
     * Remove dedicated group.
     *
     * @param group
     *      target group
     * @return
     */
    public FF4jGrantees revokeRole(String group) {
        roles.remove(group);
        return this;
    }
    
    /**
     * Group is granted.
     *
     * @param user
     *      current user
     * @return
     *      if the user if part of the grantees
     */
    public boolean isRoleGranted(String groupName) {
        return roles.contains(groupName);
    }
    
    /**
     * User is granted specifically or is member of a specialized group.
     *
     * @param user
     *      current user
     * @return
     *      if the user if part of the grantees
     */
    public boolean isUserGranted(String userName) {
        return users.contains(userName);
    }
    
    /**
     * User is granted specifically or is member of a specialized group.
     *
     * @param user
     *      current user
     * @return
     *      if the user if part of the grantees
     */
    public boolean isUserGranted(FF4jUser user) {
        return users.contains(user.getUid()) ? true :
            !roles.stream()
                    .filter(user.getRoles()::contains)
                    .collect(Collectors.toList())
                    .isEmpty();
    }

    /**
     * Getter accessor for attribute 'users'.
     *
     * @return
     *       current value of 'users'
     */
    public Set<String> getUsers() {
        return users;
    }

    /**
     * Setter accessor for attribute 'users'.
     * @param users
     * 		new value for 'users '
     */
    public void setUsers(Set<String> users) {
        this.users = users;
    }

    /**
     * Getter accessor for attribute 'roles'.
     *
     * @return
     *       current value of 'roles'
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Setter accessor for attribute 'roles'.
     * @param roles
     * 		new value for 'roles '
     */
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
    
}
