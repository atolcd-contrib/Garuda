function sendData() {
    var data = {};

    var activeLength = document.getElementById("active_rules").rows.length;
    var activeRules = [];
    for (let i = 0; i < activeLength; i++) {
        activeRules[i] = document.getElementById("active_rules").rows[i].id;
    }

    var nonActiveLength = document.getElementById("non_active_rules").rows.length;
    var nonActiveRules = [];
    for (let i = 0; i < nonActiveLength; i++) {
        nonActiveRules[i] = document.getElementById("non_active_rules").rows[i].id;
    }

    data.active_ids = activeRules;
    data.non_active_ids = nonActiveRules;
    data.csrfToken = @csrfToken;

    const XHR = new XMLHttpRequest();

    XHR.addEventListener('load', (event) => {
      alert('Yeah! Data sent and response loaded.');
    });

    XHR.addEventListener('error', (event) => {
      alert('Oops! Something went wrong.');
    });

    XHR.open('POST', '@postAffectRulesUrl');

    const boundary = "blob";

    XHR.setRequestHeader('Content-Type', `multipart/form-data; boundary=${boundary}`);

    XHR.send(data);
}

$("table tr").click(function(){
    if ($(this).hasClass('selected')) {
        $(this).removeClass('selected');
    } else {
        $(this).addClass('selected');
    }
});

$('.select_all_active').on('click', function(e) {
    $("#active_rules tr").removeClass('selected');
    $("#active_rules tr").addClass('selected');
});

$('.select_all_non_active').on('click', function(e) {
    $("#non_active_rules tr").removeClass('selected');
    $("#non_active_rules tr").addClass('selected');
});

function moveRows(from_table_id, to_table_id) {
    var rows = document.getElementById(from_table_id).getElementsByClassName('selected');
    var oldLength = document.getElementById(to_table_id).rows.length;
    for (let i = rows.length - 1; i >= 0; i--) {
        document.getElementById(to_table_id).appendChild(rows[i]);
    }
    var newLength = document.getElementById(to_table_id).rows.length;
    for (let i = oldLength; i < newLength; i++) {
        document.getElementById(to_table_id).rows[i].classList.remove('selected');
    }
}

$('.move_active_rules').on('click',function(e) {
    moveRows("active_rules", "non_active_rules");
});

$('.move_non_active_rules').on('click', function(e) {
    moveRows("non_active_rules", "active_rules");
});

$('.ok').on('click', function(e) {
    sendData();
});