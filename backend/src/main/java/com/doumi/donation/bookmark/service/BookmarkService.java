package com.doumi.donation.bookmark.service;

import com.doumi.donation.campaigns.model.dto.Campaign;

import java.util.List;

public interface BookmarkService {
    void addBookmark(long memberId, long campaignId);
    void removeBookmark(long memberId, long campaignId);
    List<Campaign> getMyBookmarks(long memberId);
}
