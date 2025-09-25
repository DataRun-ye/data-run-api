//package org.nmcpye.datarun.analytics.projection.service;
//
//import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * RawRepeatExtractor
// * <p>
// * Responsibilities:
// * - Load cached repeat metadata (repeat_uid -> semantic_path) from template_element
// * - Offer a backfill helper to iterate submissions for a single repeat
// * <p>
// *
// * @author Hamza Assada
// * @since 17/09/2025
// */
//@Service
//public class RawRepeatBackfill {
//    private final Map<String, String> repeatCache = new ConcurrentHashMap<>();
//    private final ElementTemplateConfigRepository etcRepo;
//    private final DataSubmissionRepository submissionRepo;
//    private final RawRepeatExtractor extractor;
//
//    // batch size for batch insert
//    private final int batchSize = 500;
//
//    public RawRepeatBackfill(
//        ElementTemplateConfigRepository etcRepo,
//        DataSubmissionRepository submissionRepo, RawRepeatExtractor extractor) {
//
//        this.etcRepo = etcRepo;
//        this.submissionRepo = submissionRepo;
//        this.extractor = extractor;
//        loadRepeatCacheFromRepo();
//    }
//
//    private void loadRepeatCacheFromRepo() {
//        repeatCache.clear();
//        List<Object[]> rows = etcRepo.findAllRepeats();
//        for (Object[] r : rows) {
//            String uid = (String) r[0];
//            String path = (String) r[1];
//            if (uid != null && path != null) repeatCache.put(uid, path);
//        }
//    }
//
//    /**
//     * Backfill submissions for a given repeatUid. This pages submission UIDs that contain the path and runs extractForSubmission for each.
//     * Simple OFFSET/LIMIT paging is used – acceptable for medium-sized datasets. For very large datasets, replace with cursor-based paging.
//     */
//    public void backfillRepeat(String repeatUid, int pageSize) {
//        String path = repeatCache.get(repeatUid);
//        int offset = 0;
//        while (true) {
//            List<String> uids = submissionRepo.findSubmissionUidsWithPath(path, pageSize, offset);
//            if (uids.isEmpty()) break;
//            uids.forEach(uid -> {
//                try {
//                    extractor.extractForSubmission(uid);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            });
//            offset += uids.size();
//        }
//    }
//
//    /**
//     * Force reload of repeat cache (call after you update template_element meta)
//     */
//    public void reloadRepeatCache() {
//        repeatCache.clear();
//        loadRepeatCacheFromRepo();
//    }
//}
