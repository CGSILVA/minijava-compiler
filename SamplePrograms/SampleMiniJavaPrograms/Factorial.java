class Factorial{
    public static void main(String[] a){

		System.out.println(new Fac().ComputeFac(10));
    }
}

class Fac {

    public int ComputeFac(int num){
		int num_aux ;	
		if (num < 12)
			num_aux = 1 ;
		else 
			num_aux = num * 2 ;
			
		while (num < 10) {
			num_aux = 30 ;
			num = 40;
		}
		return num_aux ;
    }

}
