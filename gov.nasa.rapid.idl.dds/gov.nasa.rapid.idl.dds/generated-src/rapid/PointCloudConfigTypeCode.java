
/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl using "rtiddsgen".
The rtiddsgen tool is part of the RTI Connext distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the RTI Connext manual.
*/

package rapid;

import com.rti.dds.typecode.*;

public class  PointCloudConfigTypeCode {
    public static final TypeCode VALUE = getTypeCode();

    private static TypeCode getTypeCode() {
        TypeCode tc = null;
        int __i=0;
        ValueMember sm[]=new ValueMember[4];

        sm[__i]=new  ValueMember("referenceFrame", false, (short)-1,  false,PUBLIC_MEMBER.VALUE,(TypeCode) rapid.String128TypeCode.VALUE,1 , false);__i++;
        sm[__i]=new  ValueMember("xyzMode", false, (short)-1,  false,PUBLIC_MEMBER.VALUE,(TypeCode) rapid.PointSampleXyzModeTypeCode.VALUE,2 , false);__i++;
        sm[__i]=new  ValueMember("attributesMode", false, (short)-1,  false,PUBLIC_MEMBER.VALUE,(TypeCode) new TypeCode(new int[] {2}, rapid.PointSampleAttributeModeTypeCode.VALUE),3 , false);__i++;
        sm[__i]=new  ValueMember("attributes", false, (short)-1,  false,PUBLIC_MEMBER.VALUE,(TypeCode) rapid.KeyTypeValueSequence16TypeCode.VALUE,4 , false);__i++;

        tc = TypeCodeFactory.TheTypeCodeFactory.create_value_tc("rapid::PointCloudConfig",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY, VM_NONE.VALUE,rapid.MessageTypeCode.VALUE, sm);        
        return tc;
    }
}

