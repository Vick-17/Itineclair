package fr.itineclair.profile;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface HikerProfileRepository extends JpaRepository<HikerProfile, UUID> {
}
