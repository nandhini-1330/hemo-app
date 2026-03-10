package com.anemia.controller;

import com.anemia.model.Analysis;
import com.anemia.model.User;
import com.anemia.repository.AnalysisRepository;
import com.anemia.service.AnemiaDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisRepository analysisRepository;
    private final AnemiaDetectionService detectionService;

    public AnalysisController(AnalysisRepository analysisRepository,
                               AnemiaDetectionService detectionService) {
        this.analysisRepository = analysisRepository;
        this.detectionService = detectionService;
    }

    @PostMapping
    public ResponseEntity<?> analyze(@AuthenticationPrincipal User user,
                                      @RequestBody AnalysisRequest req) {
        AnemiaDetectionService.DetectionResult result = detectionService.detect(
            req.hemoglobin, req.hematocrit, req.mcv, req.mch, req.mchc,
            req.rbc, req.rdw, req.serumIron, req.ferritin,
            req.transferrinSaturation, req.age, req.sex
        );
        Analysis analysis = new Analysis();
        analysis.setUser(user);
        analysis.setHemoglobin(req.hemoglobin);
        analysis.setHematocrit(req.hematocrit);
        analysis.setMcv(req.mcv);
        analysis.setMch(req.mch);
        analysis.setMchc(req.mchc);
        analysis.setRbc(req.rbc);
        analysis.setRdw(req.rdw);
        analysis.setSerumIron(req.serumIron);
        analysis.setFerritin(req.ferritin);
        analysis.setTransferrinSaturation(req.transferrinSaturation);
        analysis.setAge(req.age);
        analysis.setSex(req.sex);
        analysis.setAnemic(result.isAnemic());
        analysis.setAnemiaType(result.getAnemiaType());
        analysis.setConfidence(result.getConfidence());
        analysisRepository.save(analysis);
        return ResponseEntity.ok(toMap(analysis));
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            analysisRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toMap).collect(Collectors.toList())
        );
    }

    @GetMapping("/recent")
    public ResponseEntity<?> recent(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            analysisRepository.findTop5ByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toMap).collect(Collectors.toList())
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal User user) {
        long total = analysisRepository.countByUser(user);
        long anemic = analysisRepository.countByUserAndAnemic(user, true);
        return ResponseEntity.ok(Map.of(
            "total", total,
            "anemic", anemic,
            "normal", total - anemic
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return analysisRepository.findById(id)
            .filter(a -> a.getUser().getId().equals(user.getId()))
            .map(a -> ResponseEntity.ok(toMap(a)))
            .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Analysis a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("hemoglobin", a.getHemoglobin());
        map.put("hematocrit", a.getHematocrit());
        map.put("mcv", a.getMcv());
        map.put("mch", a.getMch());
        map.put("mchc", a.getMchc());
        map.put("rbc", a.getRbc());
        map.put("rdw", a.getRdw());
        map.put("serumIron", a.getSerumIron());
        map.put("ferritin", a.getFerritin());
        map.put("transferrinSaturation", a.getTransferrinSaturation());
        map.put("age", a.getAge());
        map.put("sex", a.getSex());
        map.put("anemic", a.isAnemic());
        map.put("anemiaType", a.getAnemiaType());
        map.put("confidence", a.getConfidence());
        map.put("createdAt", a.getCreatedAt().toString());
        return map;
    }

    static class AnalysisRequest {
        public Double hemoglobin, hematocrit, mcv, mch, mchc;
        public Double rbc, rdw, serumIron, ferritin, transferrinSaturation;
        public Integer age;
        public String sex;
    }
}