package com.doumi.donation.bookmark.service;

import com.doumi.donation.bookmark.model.dao.BookmarkDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImp implements BookmarkService {

    private final BookmarkDao bookmarkDao;

    @Override
    @Transactional
    public void addBookmark(long memberId, long campaignId) {
        if (!bookmarkDao.existsCampaign(campaignId)) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        bookmarkDao.insertBookmark(memberId, campaignId);
    }

    @Override
    @Transactional
    public void removeBookmark(long memberId, long campaignId) {
        bookmarkDao.deleteBookmark(memberId, campaignId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Campaign> getMyBookmarks(long memberId) {
        return bookmarkDao.findBookmarkedCampaigns(memberId);
    }
}
