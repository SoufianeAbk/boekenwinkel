package be.ehb.gebruikersservice.service;

import be.ehb.gebruikersservice.dto.MeldingDto;
import be.ehb.gebruikersservice.repository.MeldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeldingService {

    private final MeldingRepository meldingRepository;

    @Transactional(readOnly = true)
    public List<MeldingDto> meldingenVan(Long gebruikerId) {
        return meldingRepository.findByGebruikerIdOrderByAangemaaktOpDesc(gebruikerId).stream()
                .map(MeldingDto::van)
                .toList();
    }

    @Transactional
    public void markeerAllesGelezen(Long gebruikerId) {
        meldingRepository.markeerAllesGelezen(gebruikerId);
    }
}
