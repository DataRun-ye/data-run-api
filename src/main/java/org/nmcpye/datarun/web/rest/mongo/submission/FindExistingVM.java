package org.nmcpye.datarun.web.rest.mongo.submission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <06-05-2025>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FindExistingVM {
    private String form;
    private List<String> codes;
}
