package com.regor.backend.service;
import com.regor.backend.dto.Api.*;
import com.regor.backend.entity.*;
import com.regor.backend.repository.*;
import java.time.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
    private final PresenceService presence;
    private final ProfileRepository profiles; private final UserRepository users;
    private final PhotoRepository photos; private final UserPreferenceRepository preferences;
    public ProfileService(ProfileRepository profiles, UserRepository users, PhotoRepository photos, UserPreferenceRepository preferences,PresenceService presence) {
        this.presence=presence;this.profiles=profiles;this.users=users;this.photos=photos;this.preferences=preferences;
    }
    public ProfileView view(UUID id, UUID viewer) {
        var p=profiles.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Perfil não encontrado"));
        var u=users.findById(id).orElseThrow();
        var prefs=preferences.findById(id).orElseThrow();
        var own=profiles.findById(viewer).orElseThrow();
        Double distance=null;
        if(prefs.showDistance && p.latitude!=null && own.latitude!=null) {
            double radians=Math.sin(Math.toRadians(p.latitude))*Math.sin(Math.toRadians(own.latitude))
                +Math.cos(Math.toRadians(p.latitude))*Math.cos(Math.toRadians(own.latitude))*Math.cos(Math.toRadians(p.longitude-own.longitude));
            distance=Math.round(6371*Math.acos(Math.max(-1,Math.min(1,radians)))*10.0)/10.0;
        }
        return new ProfileView(p.id,p.displayName,p.username,Period.between(p.birthDate,LocalDate.now()).getYears(),
            id.equals(viewer)?p.birthDate:null,p.location,p.bio,p.interests,p.gender,
            id.equals(viewer)?p.latitude:null,id.equals(viewer)?p.longitude:null,u.verified,u.vip,prefs.showOnline && presence.online(id),distance,
            photos.idsForUser(id));
    }
    @Transactional
    public ProfileView update(UUID id, ProfileEdit edit) {
        AuthService.checkAge(edit.birthDate());
        if((edit.latitude()==null)!=(edit.longitude()==null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe latitude e longitude juntas");
        var p=profiles.findById(id).orElseThrow();
        p.displayName=edit.displayName().strip(); p.username=edit.username().toLowerCase(Locale.ROOT);
        p.birthDate=edit.birthDate(); p.location=edit.location().strip(); p.bio=edit.bio().strip();
        p.interests=edit.interests().strip(); p.gender=edit.gender().strip(); p.latitude=edit.latitude(); p.longitude=edit.longitude();
        profiles.flush();
        return view(id,id);
    }
    public Preferences preferences(UUID id) {
        var p=preferences.findById(id).orElseThrow();
        return new Preferences(p.minAge,p.maxAge,p.maxDistanceKm,p.interest,p.gender,p.location,
            p.matchesNotif,p.messagesNotif,p.showDistance,p.showOnline,p.discoverable,p.theme,p.language);
    }
    @Transactional
    public Preferences preferences(UUID id, Preferences edit) {
        if(edit.minAge()>edit.maxAge()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Faixa etária inválida");
        var p=preferences.findById(id).orElseThrow();
        p.minAge=edit.minAge();p.maxAge=edit.maxAge();p.maxDistanceKm=edit.maxDistanceKm();
        p.interest=edit.interest().strip();p.gender=edit.gender().strip();p.location=edit.location().strip();
        p.matchesNotif=edit.matchesNotif();p.messagesNotif=edit.messagesNotif();
        p.showDistance=edit.showDistance();p.showOnline=edit.showOnline();p.discoverable=edit.discoverable();
        p.theme=edit.theme();p.language=edit.language(); return preferences(id);
    }
    @Transactional
    public UUID upload(UUID userId, MultipartFile file) {
        users.lockById(userId).orElseThrow();
        if(photos.countByUserId(userId)>=6) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Limite de seis fotos");
        if(file.isEmpty()||file.getSize()>5*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Foto deve ter até 5 MB");
        try {
            byte[] bytes=file.getBytes();
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) throw new IOException("Imagem não reconhecida");
                var reader=readers.next();
                try {
                    reader.setInput(input);
                    String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if(!Set.of("jpeg","png").contains(format)) throw new IOException("Formato");
                    int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(w<1||h<1||w>8000||h>8000||(long)w*h>24000000) throw new IOException("Dimensões");
                    var decoded=reader.read(0);
                    var normalized=new ByteArrayOutputStream();
                    if(!ImageIO.write(decoded,format,normalized)) throw new IOException("Codificação");
                    if(normalized.size()>5*1024*1024) throw new IOException("Tamanho");
                    var photo=new Photo();photo.userId=userId;photo.content=normalized.toByteArray();
                    photo.mediaType=format.equals("png")?"image/png":"image/jpeg";photos.save(photo);
                    return photo.id;
                } finally { reader.dispose(); }
            }
        } catch(IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Envie uma imagem JPEG ou PNG válida de até 24 megapixels"); }
    }
    public Photo photo(UUID id) {
        return photos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Foto não encontrada"));
    }
    @Transactional
    public void deletePhoto(UUID userId,UUID id) {
        var photo=photo(id);
        if(!photo.userId.equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        photos.delete(photo);
    }
}
