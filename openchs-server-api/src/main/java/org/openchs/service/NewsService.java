package org.openchs.service;

import org.openchs.dao.NewsRepository;
import org.openchs.domain.News;
import org.openchs.util.BadRequestError;
import org.openchs.util.ReactAdminUtil;
import org.openchs.web.request.NewsContract;
import org.springframework.stereotype.Service;

@Service
public class NewsService {

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public News saveNews(NewsContract newsContract) {
        assertNoExistingNewsWithTitle(newsContract.getTitle());
        News news = new News();
        buildNews(newsContract, news);
        return newsRepository.save(news);
    }

    private void buildNews(NewsContract newsContract, News news) {
        news.assignUUIDIfRequired();
        news.setTitle(newsContract.getTitle());
        news.setContent(newsContract.getContent());
        news.setContentHtml(newsContract.getContentHtml());
        news.setHeroImage(newsContract.getHeroImage());
        news.setPublishedDate(newsContract.getPublishedDate());
    }

    public News editNews(NewsContract newsContract, Long id) {
        News news = newsRepository.findOne(id);
        assertNewTitleIsUnique(newsContract.getTitle(), news.getTitle());
        buildNews(newsContract, news);
        return newsRepository.save(news);
    }

    public void deleteNews(News news) {
        news.setVoided(true);
        news.setTitle(ReactAdminUtil.getVoidedName(news.getTitle(), news.getId()));
        newsRepository.save(news);
    }

    private void assertNoExistingNewsWithTitle(String title) {
        News existingNews = newsRepository.findByTitleAndIsVoidedFalse(title);
        if (existingNews != null) {
            throw new BadRequestError(String.format("News with the title %s already exists", title));
        }
    }

    private void assertNewTitleIsUnique(String newTitle, String oldTitle) {
        if (!newTitle.equals(oldTitle)) {
            assertNoExistingNewsWithTitle(newTitle);
        }
    }
}
