package com.sism.service;

import com.sism.entity.AppUser;
import com.sism.entity.Org;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.UserRepository;
import com.sism.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for user management
 * Provides methods for querying users and their organization information
 * 
 * Requirements: 8.1
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrgService orgService;

    /**
     * Get user by ID
     * 
     * @param userId user ID
     * @return user entity
     * @throws ResourceNotFoundException if user not found
     */
    public AppUser getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Find user by username
     * Requirements: 8.1 - Load user's organization and hierarchy information
     * 
     * @param username username to search
     * @return optional user entity
     */
    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by username
     * 
     * @param username username to search
     * @return user entity
     * @throws ResourceNotFoundException if user not found
     */
    public AppUser getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }


    /**
     * Get user with organization information as VO
     * Requirements: 8.1 - Load user's organization and hierarchy information
     * 
     * @param userId user ID
     * @return user value object with organization details
     */
    public UserVO getUserVO(Long userId) {
        AppUser user = getUserById(userId);
        return toUserVO(user);
    }

    /**
     * Get user VO by username
     * 
     * @param username username to search
     * @return user value object with organization details
     */
    public UserVO getUserVOByUsername(String username) {
        AppUser user = getByUsername(username);
        return toUserVO(user);
    }

    /**
     * Get all active users
     * 
     * @return list of active users
     */
    public List<UserVO> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue().stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());
    }

    /**
     * Get users by organization ID
     * 
     * @param orgId organization ID
     * @return list of users in the organization
     */
    public List<UserVO> getUsersByOrgId(Long orgId) {
        return userRepository.findByOrg_OrgIdAndIsActiveTrue(orgId).stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());
    }

    /**
     * Check if username exists
     * 
     * @param username username to check
     * @return true if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Convert AppUser entity to UserVO
     * Includes organization information
     */
    private UserVO toUserVO(AppUser user) {
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setIsActive(user.getIsActive());
        
        // Include organization information
        Org org = user.getOrg();
        if (org != null) {
            vo.setOrgId(org.getOrgId());
            vo.setOrgName(org.getOrgName());
            vo.setOrgType(org.getOrgType());
        }
        
        return vo;
    }
}
