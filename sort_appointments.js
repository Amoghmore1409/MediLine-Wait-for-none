const fs = require('fs');
const f = 'app/src/main/java/com/example/mediline/PatientHomeActivity.java';
let c = fs.readFileSync(f, 'utf8');

c = c.replace(
    '            boolean hasPrescription = false;\n            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());\n\n            for (var doc : querySnapshot.getDocuments()) {\n                Appointment appt = doc.toObject(Appointment.class);',
    \            boolean hasPrescription = false;\n            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());\n\n            List<Appointment> sortedAppointments = new ArrayList<>();\n            for (var doc : querySnapshot.getDocuments()) {\n                Appointment a = doc.toObject(Appointment.class);\n                if (a != null) {\n                    a.setAppointmentId(doc.getId());\n                    sortedAppointments.add(a);\n                }\n            }\n            Collections.sort(sortedAppointments, (a1, a2) -> {\n                if (a1.getCreatedAt() == null && a2.getCreatedAt() == null) return 0;\n                if (a1.getCreatedAt() == null) return 1;\n                if (a2.getCreatedAt() == null) return -1;\n                return a2.getCreatedAt().compareTo(a1.getCreatedAt());\n            });\n\n            for (Appointment appt : sortedAppointments) {\
);

c = c.replace(
    '            for (var doc : querySnapshot.getDocuments()) {\n                Appointment appt = doc.toObject(Appointment.class);\n                if (appt != null) {\n                    appt.setAppointmentId(doc.getId());\n\n                    android.view.View item = getLayoutInflater().inflate(R.layout.item_patient_appointment, container, false);',
    \            List<Appointment> sortedAppointments = new ArrayList<>();\n            for (var doc : querySnapshot.getDocuments()) {\n                Appointment a = doc.toObject(Appointment.class);\n                if (a != null) {\n                    a.setAppointmentId(doc.getId());\n                    sortedAppointments.add(a);\n                }\n            }\n            Collections.sort(sortedAppointments, (a1, a2) -> {\n                if (a1.getCreatedAt() == null && a2.getCreatedAt() == null) return 0;\n                if (a1.getCreatedAt() == null) return 1;\n                if (a2.getCreatedAt() == null) return -1;\n                return a2.getCreatedAt().compareTo(a1.getCreatedAt());\n            });\n\n            for (Appointment appt : sortedAppointments) {\n                if (appt != null) {\n                    android.view.View item = getLayoutInflater().inflate(R.layout.item_patient_appointment, container, false);\
);

fs.writeFileSync(f, c);
console.log('patched');
