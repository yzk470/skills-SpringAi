package org.example.skillsspringai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScriptResult {
    private boolean success;
    private String output;

    public static ScriptResult success(String output) {
        return new ScriptResult(true, output);
    }

    public static ScriptResult error(String output) {
        return new ScriptResult(false, output);
    }
}
