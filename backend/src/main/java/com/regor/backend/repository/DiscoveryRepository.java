package com.regor.backend.repository;
import com.regor.backend.entity.Profile;
import com.regor.backend.dto.Api.Preferences;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class DiscoveryRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public DiscoveryRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<UUID> discover(UUID user, Profile own, Preferences prefs, String query, Integer minAge,
            Integer maxAge, String location, String interest, String gender, Integer distance, int page) {
        int min=minAge==null?prefs.minAge():minAge, max=maxAge==null?prefs.maxAge():maxAge;
        StringBuilder sql=new StringBuilder("""
            SELECT p.id FROM profiles p JOIN user_preferences pref ON pref.id=p.id
            WHERE p.id <> :user AND pref.discoverable=true
            AND date_part('year',age(current_date,p.birth_date)) BETWEEN :min AND :max
            AND NOT EXISTS(SELECT 1 FROM swipes s WHERE s.actor_id=:user AND s.target_id=p.id)
            """);
        var args=new MapSqlParameterSource().addValue("user",user).addValue("min",min).addValue("max",max)
            .addValue("offset",page*30);
        addFilter(sql,args,"p.location","location",location==null?prefs.location():location);
        addFilter(sql,args,"p.interests","interest",interest==null?prefs.interest():interest);
        addFilter(sql,args,"p.gender","gender",gender==null?prefs.gender():gender);
        if(query!=null&&!query.isBlank()) {
            sql.append(" AND (lower(p.display_name) LIKE :q OR lower(p.interests) LIKE :q)");
            args.addValue("q","%"+escape(query.toLowerCase(Locale.ROOT))+"%");
        }
        Integer km=distance;
        if(km==null&&own.latitude!=null) km=prefs.maxDistanceKm();
        if(km!=null) {
            if(own.latitude==null) return List.of();
            sql.append("""
                 AND p.latitude IS NOT NULL AND
                 6371 * acos(least(1.0,greatest(-1.0,
                 sin(radians(:lat))*sin(radians(p.latitude))+
                 cos(radians(:lat))*cos(radians(p.latitude))*cos(radians(p.longitude-:lon))))) <= :km
                """);
            args.addValue("lat",own.latitude).addValue("lon",own.longitude).addValue("km",km);
        }
        sql.append(" ORDER BY p.id LIMIT 30 OFFSET :offset");
        return jdbc.query(sql.toString(),args,(rs,n)->rs.getObject(1,UUID.class));
    }
    private static void addFilter(StringBuilder sql,MapSqlParameterSource args,String column,String key,String value) {
        if(value!=null&&!value.isBlank()) {
            sql.append(" AND lower(").append(column).append(") LIKE :").append(key);
            args.addValue(key,"%"+escape(value.toLowerCase(Locale.ROOT))+"%");
        }
    }
    private static String escape(String s) { return s.replace("\\","\\\\").replace("%","\\%").replace("_","\\_"); }
}
