package ubb.proiectColectiv.businessmanagementbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.var;
import org.springframework.stereotype.Service;
import ubb.proiectColectiv.businessmanagementbackend.model.User;
import ubb.proiectColectiv.businessmanagementbackend.model.UserSkill;
import ubb.proiectColectiv.businessmanagementbackend.utils.FirebaseUtils;

import java.io.IOException;
import java.util.*;

@Service
public class SupervisorService {

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Retrieves users which have approvedStatus == false
     *
     * @return list of users with appreovedStatus == false
     */
    public List<User> getRegistrationRequests() {
        List<User> unregisteredUsers = new ArrayList<>();
        HashMap<String, User> users = (HashMap) FirebaseUtils.getUpstreamData(Arrays.asList("User"));
        User user;
        for (Map.Entry<String, User> entry : users.entrySet()) {
            user = mapper.convertValue(entry.getValue(), User.class);
            if (user.getApprovedStatus() == false) {
                user.setHashedEmail(entry.getKey());
                unregisteredUsers.add(user);
            }
        }
        return unregisteredUsers;
    }

    /**
     * Retrieves users which have approvedProfileChange == false
     *
     * @return list of users with appreovedProfileChange == false
     */
    public List<User> getProfileEdits(String id) {
        List<User> profileEdits = new ArrayList<>();
        List<User> supervisedUsers = getUsersForSupervisor(id);
        for (User user : supervisedUsers) {
            if (user.getEdits() != null) {
                profileEdits.add(user);
            }
        }
        return profileEdits;
    }

    /**
     * Checks if user is a supervisor
     *
     * @param hashedEmail
     * @return true if user is supervisor, false otherwise
     */
    public Boolean isSupervisor(String hashedEmail) {
        HashMap<String, Object> user = (HashMap) FirebaseUtils.getUpstreamData(Arrays.asList("User", hashedEmail));
        if (user.get("roleId").toString().compareTo("2") == 0) {
            return true;
        }
        return false;
    }

    // TODO: 11-Dec-19 documentation
    public List<User> getUsersForSupervisor(String id) {
        List<User> retList = new ArrayList<>();
        if (!isSupervisor(id)) {
            return null;
        }
        HashMap<String, String> supervisedUsers = (HashMap) FirebaseUtils.getUpstreamData(Arrays.asList("User", id, "usersList"));
        for (String email : supervisedUsers.values()) {
            HashMap<String, Object> userAsMap = (HashMap) FirebaseUtils.getUpstreamData(Arrays.asList("User", String.valueOf(Objects.hash(email))));
            User user = mapper.convertValue(userAsMap, User.class);
            user.setHashedEmail(String.valueOf(Objects.hash(email)));
            retList.add(user);
        }
        return retList;
    }

    /**
     * Sets users approvedStatus to true
     *
     * @param json Json containing users hashed email
     * @throws JsonProcessingException If the recieved json is incorrect
     */
    public void approveRegistrationRequest(String json) throws JsonProcessingException {
        HashMap<String, String> map = mapper.readValue(json, HashMap.class);
        FirebaseUtils.setValue(Arrays.asList("User", map.get("hashedEmail"), "approvedStatus"), true);
    }

    /**
     * Deletes user from Firebase
     *
     * @param json Json containing users hashed email
     * @throws JsonProcessingException If the recieved json is incorrect
     */
    public void rejectRegistrationRequest(String json) throws JsonProcessingException {
        HashMap<String, String> map = mapper.readValue(json, HashMap.class);
        FirebaseUtils.removeValue(Arrays.asList("User", map.get("hashedEmail")));
    }

    /**
     * Retrieves users which have blockedStatus == false
     *
     * @return list of users with blockedStatus == true
     */
    public List<User> getBlockedUsers() {
        List<User> blockedUsers = new ArrayList<>();
        HashMap<String, User> users = (HashMap) FirebaseUtils.getUpstreamData(Arrays.asList("User"));
        User user;
        for (Map.Entry<String, User> entry : users.entrySet()) {
            user = mapper.convertValue(entry.getValue(), User.class);
            if (user.getBlockedStatus() == true) {
                user.setHashedEmail(entry.getKey());
                blockedUsers.add(user);
            }
        }
        return blockedUsers;
    }

    /**
     * Sets users blockedStatus to true
     *
     * @param json Json containing users hashed email
     * @throws JsonProcessingException If the recieved json is incorrect
     */
    public void approveBlockedUser(String json) throws JsonProcessingException {
        HashMap<String, String> map = mapper.readValue(json, HashMap.class);
        FirebaseUtils.setValue(Arrays.asList("User", map.get("hashedEmail"), "blockedStatus"), false);
        FirebaseUtils.setValue(Arrays.asList("User", map.get("hashedEmail"), "failedLoginCounter"), 0);
    }

    /**
     * Deletes user from Firebase
     *
     * @param json Json containing users hashed email
     * @throws JsonProcessingException If the recieved json is incorrect
     */
    public void rejectBlockedUser(String json) throws JsonProcessingException {
        HashMap<String, String> map = mapper.readValue(json, HashMap.class);
        FirebaseUtils.removeValue(Arrays.asList("User", map.get("hashedEmail")));
    }

    /**
     * Get users that have the skill with that skillId
     *
     * @param skillId skill to be searched after
     * @return list of users with that skill
     */
    public List<User> getUsersBySkill(String skillId) throws IOException {
        List<User> usersWithThatSkill = new ArrayList<User>();
        HashMap<String, User> users = (HashMap) FirebaseUtils.getUpstreamData(Arrays.asList("User"));
        var userSkills = FirebaseUtils.getCollectionAsUpstreamData(Arrays.asList("UserSkills"), false, HashMap.class);
        List<UserSkill> userSkillList = mapper.convertValue(userSkills, new TypeReference<List<UserSkill>>() {
        });
        for (var skill : userSkillList) {
            if (skillId.equals(skill.getSkillId())) {
                User user = mapper.convertValue(users.get(skill.getUserHash()), User.class);
                usersWithThatSkill.add(user);
            }
        }
        return usersWithThatSkill;
    }
}
