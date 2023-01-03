$('.card').click(function(){
  $(this).toggleClass('active');
});


$('.notification-close').on('click', notificationClose);

function notificationClose() {
  $(this)
    .parent('.notification')
    .hide();
};