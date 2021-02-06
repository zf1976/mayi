package com.zf1976.ant.upms.biz.exp;

import com.zf1976.ant.upms.biz.exp.base.SysBaseException;
import com.zf1976.ant.upms.biz.exp.enums.DepartmentState;

/**
 * @author mac
 * @date 2020/12/17
 **/
public class DepartmentException extends SysBaseException {

    public DepartmentException(DepartmentState state) {
        super(state.getValue(), state.getReasonPhrase());
    }

    public DepartmentException(DepartmentState state,String label) {
        this(state);
        super.label = label;
    }
}
