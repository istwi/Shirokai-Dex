package br.com.shirokaidex.captures;

import java.sql.*;
import java.util.*;

public final class CapturedForms {
    private CapturedForms() { }
    public record Form(String species,String form) {public Form{br.com.shirokaidex.pokedex.CollectionDefinition.identifier(species);Objects.requireNonNull(form);form=form.toLowerCase(Locale.ROOT);}}
    public static void record(Connection c,CaptureRecord capture)throws SQLException {
        if(!capture.verified())return;try(var q=c.prepareStatement("INSERT INTO captured_forms VALUES(?,?,?,?,?) ON CONFLICT(player_uuid,species,form) DO UPDATE SET first_captured_at=min(first_captured_at,excluded.first_captured_at),shiny=max(shiny,excluded.shiny)")){
            q.setString(1,capture.player().toString());q.setString(2,capture.species());q.setString(3,capture.form().toLowerCase(Locale.ROOT));q.setLong(4,capture.capturedAt().toEpochMilli());q.setBoolean(5,capture.shiny());q.executeUpdate();
        }
    }
    public static Set<Form> forms(Connection c,UUID player)throws SQLException {
        var result=new HashSet<Form>();try(var q=c.prepareStatement("SELECT species,form FROM captured_forms WHERE player_uuid=?")){q.setString(1,player.toString());try(var rows=q.executeQuery()){while(rows.next())result.add(new Form(rows.getString(1),rows.getString(2)));}}return Set.copyOf(result);
    }
}
